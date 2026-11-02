package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.DatabaseBackupManager
import com.example.data.db.AppDatabase
import com.example.data.model.Student
import com.example.data.model.StudentScore
import com.example.data.repository.ScoreRepository
import com.example.domain.ClassGradeAnalysis
import com.example.domain.GradeAnalysisTools
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val CURRICULUM_SUBJECTS = listOf(
    "English",
    "Mathematics",
    "Religious and Moral Education (R.M.E.)",
    "Ghanaian Language",
    "Science",
    "Creative Art",
    "History",
    "Computing"
)

data class ScoreFormUiState(
    val studentName: String = "",
    val assessmentName: String = "",
    val subject: String = "English",
    val scoreInput: String = "",
    val category: String = "Quiz",
    val notes: String = "",
    val maxScoreInput: String = "100",
    val editingScoreId: Long? = null,
    val studentNameError: String? = null,
    val assessmentNameError: String? = null,
    val scoreError: String? = null,
    val isFormSubmitted: Boolean = false,
    val isSaving: Boolean = false
)

sealed class ScoreUiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : ScoreUiEvent()
    object ScoreSaved : ScoreUiEvent()
}

class ScoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScoreRepository
    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ScoreRepository(database.scoreDao(), database.studentDao())
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    val allScores: StateFlow<List<StudentScore>> = repository.allScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allStudents: StateFlow<List<Student>> = repository.allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allStudentNames: StateFlow<List<String>> = combine(
        repository.allRosterStudentNames,
        repository.allStudentNamesFromScores
    ) { rosterNames, scoreNames ->
        (rosterNames + scoreNames).distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availableSubjects: StateFlow<List<String>> = allScores.map { scores ->
        val fromDb = scores.map { it.subject }.filter { it.isNotBlank() }
        (CURRICULUM_SUBJECTS + fromDb).distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CURRICULUM_SUBJECTS
    )

    // Form State
    private val _formState = MutableStateFlow(ScoreFormUiState())
    val formState: StateFlow<ScoreFormUiState> = _formState.asStateFlow()

    // Events
    private val _uiEvents = MutableSharedFlow<ScoreUiEvent>()
    val uiEvents: SharedFlow<ScoreUiEvent> = _uiEvents.asSharedFlow()

    // Filters and search for assessment list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow("All")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    // Selected student for detailed historical progress
    private val _selectedStudent = MutableStateFlow<String?>(null)
    val selectedStudent: StateFlow<String?> = _selectedStudent.asStateFlow()

    // Filtered scores based on search query (name, category, subject, title) and category/subject filters
    val filteredScores: StateFlow<List<StudentScore>> = combine(
        allScores,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedSubjectFilter
    ) { scores, query, category, subject ->
        val cleanQuery = query.trim()
        scores.filter { score ->
            val matchesQuery = cleanQuery.isBlank() ||
                score.studentName.contains(cleanQuery, ignoreCase = true) ||
                score.assessmentName.contains(cleanQuery, ignoreCase = true) ||
                score.category.contains(cleanQuery, ignoreCase = true) ||
                score.subject.contains(cleanQuery, ignoreCase = true)

            val matchesCategory = category == "All" || score.category.equals(category, ignoreCase = true)
            val matchesSubject = subject == "All" || score.subject.equals(subject, ignoreCase = true)

            matchesQuery && matchesCategory && matchesSubject
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Automated grade analysis
    val classAnalysis: StateFlow<ClassGradeAnalysis> = combine(
        allScores,
        _selectedCategoryFilter,
        _selectedSubjectFilter
    ) { scores, category, subject ->
        val relevantScores = scores.filter { score ->
            (category == "All" || score.category.equals(category, ignoreCase = true)) &&
            (subject == "All" || score.subject.equals(subject, ignoreCase = true))
        }
        GradeAnalysisTools.computeClassAnalysis(relevantScores)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GradeAnalysisTools.computeClassAnalysis(emptyList())
    )

    // Form Input Handlers with validation
    fun onStudentNameChange(name: String) {
        _formState.update {
            it.copy(
                studentName = name,
                studentNameError = if (it.isFormSubmitted) validateStudentName(name) else null
            )
        }
    }

    fun onAssessmentNameChange(name: String) {
        _formState.update {
            it.copy(
                assessmentName = name,
                assessmentNameError = if (it.isFormSubmitted) validateAssessmentName(name) else null
            )
        }
    }

    fun onSubjectChange(subject: String) {
        _formState.update { it.copy(subject = subject) }
    }

    fun onScoreChange(score: String) {
        val filtered = score.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return

        _formState.update {
            it.copy(
                scoreInput = filtered,
                scoreError = if (it.isFormSubmitted) validateScore(filtered) else null
            )
        }
    }

    fun onCategoryChange(category: String) {
        _formState.update { it.copy(category = category) }
    }

    fun onNotesChange(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    fun setQuickScore(value: Double) {
        val strVal = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        onScoreChange(strVal)
    }

    fun adjustScore(delta: Double) {
        val current = _formState.value.scoreInput.toDoubleOrNull() ?: 0.0
        val newScore = (current + delta).coerceIn(0.0, 100.0)
        setQuickScore(newScore)
    }

    // Validation Logic
    private fun validateStudentName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Student name is required"
            trimmed.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }
    }

    private fun validateAssessmentName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Assessment name is required"
            trimmed.length < 2 -> "Assessment name must be at least 2 characters"
            else -> null
        }
    }

    private fun validateScore(scoreStr: String): String? {
        val trimmed = scoreStr.trim()
        if (trimmed.isEmpty()) {
            return "Score is required"
        }
        val scoreVal = trimmed.toDoubleOrNull()
            ?: return "Please enter a valid numeric score"

        return when {
            scoreVal < 0.0 -> "Score cannot be less than 0"
            scoreVal > 100.0 -> "Score cannot exceed 100 (range: 0 - 100)"
            else -> null
        }
    }

    fun saveScore(onSuccess: (() -> Unit)? = null) {
        val current = _formState.value
        val nameErr = validateStudentName(current.studentName)
        val assessmentErr = validateAssessmentName(current.assessmentName)
        val scoreErr = validateScore(current.scoreInput)

        _formState.update {
            it.copy(
                isFormSubmitted = true,
                studentNameError = nameErr,
                assessmentNameError = assessmentErr,
                scoreError = scoreErr
            )
        }

        if (nameErr != null || assessmentErr != null || scoreErr != null) {
            viewModelScope.launch {
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Please fix the errors in the form"))
            }
            return
        }

        val numericScore = current.scoreInput.toDouble()
        val scoreEntity = StudentScore(
            id = current.editingScoreId ?: 0L,
            studentName = current.studentName.trim(),
            assessmentName = current.assessmentName.trim(),
            subject = current.subject.trim().ifEmpty { "Mathematics" },
            score = numericScore,
            maxScore = 100.0,
            category = current.category,
            notes = current.notes.trim(),
            dateMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            if (current.editingScoreId != null && current.editingScoreId != 0L) {
                repository.updateScore(scoreEntity)
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Updated ${scoreEntity.subject} score for ${scoreEntity.studentName}"))
            } else {
                repository.insertScore(scoreEntity)
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Recorded ${scoreEntity.subject} score for ${scoreEntity.studentName} (${scoreEntity.score.toInt()}%)"))
            }
            _formState.value = ScoreFormUiState() // Reset
            _uiEvents.emit(ScoreUiEvent.ScoreSaved)
            onSuccess?.invoke()
        }
    }

    fun editScore(score: StudentScore) {
        _formState.value = ScoreFormUiState(
            studentName = score.studentName,
            assessmentName = score.assessmentName,
            subject = score.subject,
            scoreInput = if (score.score % 1.0 == 0.0) score.score.toInt().toString() else score.score.toString(),
            category = score.category,
            notes = score.notes,
            editingScoreId = score.id,
            isFormSubmitted = false
        )
    }

    fun deleteScore(score: StudentScore) {
        viewModelScope.launch {
            repository.deleteScore(score)
            _uiEvents.emit(
                ScoreUiEvent.ShowSnackbar(
                    message = "Deleted assessment for ${score.studentName}",
                    actionLabel = "Undo",
                    onAction = {
                        viewModelScope.launch {
                            repository.insertScore(score)
                        }
                    }
                )
            )
        }
    }

    fun resetForm() {
        _formState.value = ScoreFormUiState()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterChange(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun onSubjectFilterChange(subject: String) {
        _selectedSubjectFilter.value = subject
    }

    fun selectStudentForProgress(studentName: String?) {
        _selectedStudent.value = studentName
    }

    fun addStudentsBatch(names: List<String>, source: String) {
        if (names.isEmpty()) return
        val newStudents = names.map {
            Student(name = it.trim(), source = source)
        }
        viewModelScope.launch {
            repository.insertStudents(newStudents)
            _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Added ${newStudents.size} students to class roster"))
        }
    }

    fun addSingleStudent(name: String, studentId: String = "") {
        val trimmed = name.trim()
        if (trimmed.length < 2) return
        val student = Student(name = trimmed, studentId = studentId.trim(), source = "Manual")
        viewModelScope.launch {
            repository.insertStudent(student)
            _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Enrolled $trimmed in class roster"))
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Removed ${student.name} from roster"))
        }
    }

    // Database & Backup Management (Phone Storage & Google Drive)
    private val _isDbBusy = MutableStateFlow(false)
    val isDbBusy: StateFlow<Boolean> = _isDbBusy.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    fun exportDatabaseToJson(context: Context, destinationUri: Uri, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isDbBusy.value = true
            try {
                val currentStudents = repository.getAllStudentsList()
                val currentScores = repository.getAllScoresList()
                val result = DatabaseBackupManager.exportDatabaseToJson(
                    context = context,
                    uri = destinationUri,
                    students = currentStudents,
                    scores = currentScores
                )
                if (result.isSuccess) {
                    _lastBackupTime.value = System.currentTimeMillis()
                    _uiEvents.emit(
                        ScoreUiEvent.ShowSnackbar(
                            "Database saved! ${currentScores.size} scores & ${currentStudents.size} students exported."
                        )
                    )
                    onSuccess?.invoke()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                    _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Export failed: $err"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Export error: ${e.localizedMessage}"))
            } finally {
                _isDbBusy.value = false
            }
        }
    }

    fun importDatabaseFromJson(context: Context, sourceUri: Uri, replaceExisting: Boolean, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isDbBusy.value = true
            try {
                val result = DatabaseBackupManager.importDatabaseFromJson(context, sourceUri)
                if (result.isSuccess) {
                    val (students, scores) = result.getOrThrow()
                    if (students.isEmpty() && scores.isEmpty()) {
                        _uiEvents.emit(ScoreUiEvent.ShowSnackbar("No student or score records found in selected file"))
                    } else {
                        repository.restoreDatabase(
                            students = students,
                            scores = scores,
                            replaceExisting = replaceExisting
                        )
                        val modeStr = if (replaceExisting) "Replaced database with" else "Merged"
                        _uiEvents.emit(
                            ScoreUiEvent.ShowSnackbar(
                                "$modeStr ${scores.size} assessments and ${students.size} student profiles."
                            )
                        )
                        onSuccess?.invoke()
                    }
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Invalid backup file"
                    _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Restore failed: $err"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Restore error: ${e.localizedMessage}"))
            } finally {
                _isDbBusy.value = false
            }
        }
    }

    fun exportGradebookToCsv(context: Context, destinationUri: Uri, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isDbBusy.value = true
            try {
                val currentScores = repository.getAllScoresList()
                val currentStudents = repository.getAllStudentsList()
                val result = DatabaseBackupManager.exportGradebookToCsv(
                    context = context,
                    uri = destinationUri,
                    scores = currentScores,
                    students = currentStudents
                )
                if (result.isSuccess) {
                    _uiEvents.emit(
                        ScoreUiEvent.ShowSnackbar(
                            "Exported ${currentScores.size} records to CSV spreadsheet successfully!"
                        )
                    )
                    onSuccess?.invoke()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Error creating CSV"
                    _uiEvents.emit(ScoreUiEvent.ShowSnackbar("CSV export failed: $err"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("CSV export error: ${e.localizedMessage}"))
            } finally {
                _isDbBusy.value = false
            }
        }
    }

    fun resetDatabaseToDefaults(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isDbBusy.value = true
            try {
                repository.clearDatabase()
                AppDatabase.populateInitialStudents(AppDatabase.getDatabase(getApplication(), viewModelScope).studentDao())
                AppDatabase.populateInitialScores(AppDatabase.getDatabase(getApplication(), viewModelScope).scoreDao())
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Database reset to curriculum sample records"))
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Reset error: ${e.localizedMessage}"))
            } finally {
                _isDbBusy.value = false
            }
        }
    }

    fun clearEntireDatabase(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isDbBusy.value = true
            try {
                repository.clearDatabase()
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Database cleared. All student and score records removed."))
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiEvents.emit(ScoreUiEvent.ShowSnackbar("Error clearing database: ${e.localizedMessage}"))
            } finally {
                _isDbBusy.value = false
            }
        }
    }
}
