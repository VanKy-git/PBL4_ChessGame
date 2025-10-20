//package com.chatapp.server.Model.DAO;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.EntityTransaction;
//import jakarta.persistence.TypedQuery;
//import java.util.List;
//
///**
// * Base DAO - Chứa các method CRUD cơ bản cho tất cả Entity
// * Generic Class để tái sử dụng cho nhiều Entity khác
// */
//public abstract class baseDAO<T> {
//    protected EntityManager entityManager;
//    private Class<T> entityClass;
//
//    public baseDAO(EntityManager entityManager, Class<T> entityClass) {
//        this.entityManager = entityManager;
//        this.entityClass = entityClass;
//    }
//
//    // ========== TRANSACTION HELPER ==========
//
//    /**
//     * Thực hiện transaction an toàn với callback
//     */
//    protected <R> R executeInTransaction(TransactionCallback<R> callback) {
//        EntityTransaction transaction = entityManager.getTransaction();
//        try {
//            transaction.begin();
//            R result = callback.execute();
//            transaction.commit();
//            return result;
//        } catch (Exception e) {
//            if (transaction.isActive()) {
//                transaction.rollback();
//            }
//            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
//        }
//    }
//
//    @FunctionalInterface
//    protected interface TransactionCallback<R> {
//        R execute();
//    }
//
//    // ========== BASIC CRUD OPERATIONS ==========
//
//    /**
//     * CREATE - Thêm entity mới
//     */
//    public T create(T entity) {
//        return executeInTransaction(() -> {
//            entityManager.persist(entity);
//            entityManager.flush();
//            return entity;
//        });
//    }
//
//    /**
//     * READ - Tìm entity theo ID
//     */
//    public T findById(Object id) {
//        return entityManager.find(entityClass, id);
//    }
//
//    /**
//     * READ - Lấy tất cả entity
//     */
//    public List<T> findAll() {
//        String queryString = "SELECT e FROM " + entityClass.getSimpleName() + " e";
//        TypedQuery<T> query = entityManager.createQuery(queryString, entityClass);
//        return query.getResultList();
//    }
//
//    /**
//     * UPDATE - Cập nhật entity
//     */
//    public T update(T entity) {
//        return executeInTransaction(() -> {
//            return entityManager.merge(entity);
//        });
//    }
//
//    /**
//     * DELETE - Xóa entity theo ID
//     */
//    public boolean deletebyID(Object id) {
//        return executeInTransaction(() -> {
//            T entity = entityManager.find(entityClass, id);
//            if (entity != null) {
//                entityManager.remove(entity);
//                return true;
//            }
//            return false;
//        });
//    }
//
//    /**
//     * DELETE - Xóa entity
//     */
//    public boolean delete(T entity) {
//        return executeInTransaction(() -> {
//            if (entity != null) {
//                T managedEntity = entityManager.merge(entity);
//                entityManager.remove(managedEntity);
//                return true;
//            }
//            return false;
//        });
//    }
//
//    /**
//     * COUNT - Đếm tổng số entity
//     */
//    public long count() {
//        String queryString = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
//        TypedQuery<Long> query = entityManager.createQuery(queryString, Long.class);
//        return query.getSingleResult();
//    }
//
//    /**
//     * EXISTS - Kiểm tra entity có tồn tại không
//     */
//    public boolean exists(Object id) {
//        T entity = entityManager.find(entityClass, id);
//        return entity != null;
//    }
//
//    // ========== PAGINATION ==========
//
//    /**
//     * Lấy danh sách với phân trang
//     */
//    public List<T> findWithPagination(int page, int pageSize) {
//        String queryString = "SELECT e FROM " + entityClass.getSimpleName() + " e";
//        TypedQuery<T> query = entityManager.createQuery(queryString, entityClass);
//        query.setFirstResult((page - 1) * pageSize);
//        query.setMaxResults(pageSize);
//        return query.getResultList();
//    }
//
//    // ========== UTILITY METHODS ==========
//
//    /**
//     * Refresh entity từ database
//     */
//    public void refresh(T entity) {
//        entityManager.refresh(entity);
//    }
//
//    /**
//     * Detach entity khỏi persistence context
//     */
//    public void detach(T entity) {
//        entityManager.detach(entity);
//    }
//
//    /**
//     * Clear persistence context
//     */
//    public void clear() {
//        entityManager.clear();
//    }
//
//    /**
//     * Flush changes to database
//     */
//    public void flush() {
//        entityManager.flush();
//    }
//}