package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.CategoryRequest;
import com.finance.dashboard.dto.response.CategoryResponse;

import java.util.List;

/**
 * Contract for expense category management.
 *
 * Categories are shared across all users.
 * Write operations are restricted to MANAGER and ADMIN at the controller level.
 *
 * Implementation: {@link com.finance.dashboard.service.impl.CategoryServiceImpl}
 */
public interface CategoryService {

    /** Creates a new category. Names must be unique (case-insensitive). */
    CategoryResponse createCategory(CategoryRequest request);

    /** Returns a category by ID. */
    CategoryResponse getCategoryById(Long id);

    /** Returns all categories (used by any authenticated user to populate dropdowns). */
    List<CategoryResponse> getAllCategories();

    /**
     * Updates a category's name.
     * The new name must not conflict with any other existing category.
     */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /**
     * Deletes a category.
     * Fails if any expense records still reference this category,
     * preserving referential integrity without a hard database constraint.
     */
    void deleteCategory(Long id);
}
