package com.finance.dashboard.service.impl;

import com.finance.dashboard.dto.request.CategoryRequest;
import com.finance.dashboard.dto.response.CategoryResponse;
import com.finance.dashboard.entity.Category;
import com.finance.dashboard.exception.AppException;
import com.finance.dashboard.repository.CategoryRepository;
import com.finance.dashboard.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Category management service.
 *
 * Categories are shared across the entire system (not per-user).
 * Write access is restricted to MANAGER and ADMIN at the controller level.
 *
 * Invariant: a category cannot be deleted while expenses still reference it.
 * This is enforced here rather than relying on a database FK constraint,
 * so that a meaningful business-level error message is returned instead of
 * a raw SQL integrity violation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AppException.ConflictException(
                    "Category already exists: " + request.getName());
        }
        Category saved = categoryRepository.save(
                Category.builder().name(request.getName()).build());
        log.info("Category created: {}", saved.getName());
        return CategoryResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return CategoryResponse.from(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findEntityById(id);

        // Allow the same name on the same record (idempotent update);
        // reject only if the new name is taken by a *different* category.
        boolean nameConflict = !category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCase(request.getName());

        if (nameConflict) {
            throw new AppException.ConflictException(
                    "Category name already taken: " + request.getName());
        }

        category.setName(request.getName());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findEntityById(id);

        if (category.getExpenses() != null && !category.getExpenses().isEmpty()) {
            throw new AppException.BadRequestException(
                    "Cannot delete category '" + category.getName() +
                    "': it has " + category.getExpenses().size() + " associated expense(s). " +
                    "Reassign or delete those expenses first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: {}", category.getName());
    }

    // ── Package-visible helper ────────────────────────────────────────────────

    /**
     * Looks up a Category entity by ID and throws a 404 if not found.
     * Used internally and by {@link ExpenseServiceImpl} to validate categoryId on expense operations.
     *
     * Refactoring note:
     * The original code had ExpenseServiceImpl injecting CategoryServiceImpl (the concrete class)
     * just to call this method. By adding it here and having ExpenseServiceImpl use the
     * {@link CategoryService} interface, both classes now depend on abstractions.
     */
    public Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException.ResourceNotFoundException("Category", id));
    }
}
