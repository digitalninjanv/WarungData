package com.warungdata.pos.features.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val nameInput: String = "",
    val nameError: String? = null
)

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val categoryDao = db.categoryDao()

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    isLoading = false
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingCategory = null,
            nameInput = "",
            nameError = null
        )
    }

    fun showEditDialog(category: CategoryEntity) {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingCategory = category,
            nameInput = category.name,
            nameError = null
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false)
    }

    fun updateNameInput(name: String) {
        _uiState.value = _uiState.value.copy(nameInput = name, nameError = null)
    }

    fun saveCategory() {
        val state = _uiState.value
        val name = state.nameInput.trim()

        if (name.isEmpty()) {
            _uiState.value = state.copy(nameError = "Nama kategori tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            val existing = categoryDao.getCategoryByName(name)
            if (existing != null && existing.id != state.editingCategory?.id) {
                _uiState.value = state.copy(nameError = "Kategori sudah ada")
                return@launch
            }

            if (state.editingCategory != null) {
                categoryDao.updateCategory(state.editingCategory.copy(name = name))
            } else {
                categoryDao.insertCategory(CategoryEntity(name = name))
            }
            hideDialog()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }
}
