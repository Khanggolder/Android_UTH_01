package com.uth.taskmanagement.ui.tasklist

import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.utils.TaskDueDateFilter
import com.uth.taskmanagement.utils.TaskPriorityFilter
import com.uth.taskmanagement.utils.TaskSortOption
import com.uth.taskmanagement.utils.TaskStatusFilter

/**
 * Trang thai hien thi cho man hinh danh sach task (ui/tasklist).
 * ket hop voi bo loc/sap xep hien tai;
 */
sealed class TaskListUiState {

    /** Dang tai du lieu tu Room lan dau. */
    data object Loading : TaskListUiState()

    /**
     * Da tai xong va co it nhat 1 task sau khi loc/sap xep.
     * @param tasks danh sach task da duoc loc + sap xep, san sang de hien thi.
     * @param overdueTaskIds tap id cac task dang qua han, dung de UI to mau/canh bao rieng.
     * @param appliedFilter bo loc trang thai dang duoc ap dung.
     * @param appliedSort kieu sap xep dang duoc ap dung.
     */
    data class Success(
        val tasks: List<TaskEntity>,
        val overdueTaskIds: Set<Long>,
        val appliedStatusFilter: TaskStatusFilter,
        val appliedPriorityFilter: TaskPriorityFilter,
        val appliedDueDateFilter: TaskDueDateFilter,
        val appliedSort: TaskSortOption
    ) : TaskListUiState()

    /**
     * Da tai xong nhung khong co task nao phu hop.
     * @param isBecauseOfFilter true = do bo loc dang ap dung khong co task nao khop
     *        (vi du loc COMPLETED nhung chua task nao xong); false = toan bo danh sach rong.
     */
    data class Empty(
        val isBecauseOfFilter: Boolean,
        val appliedStatusFilter: TaskStatusFilter,
        val appliedPriorityFilter: TaskPriorityFilter,
        val appliedDueDateFilter: TaskDueDateFilter
    ) : TaskListUiState()

    /** Co loi khi doc du lieu (vi du Room throw exception). */
    data class Error(
        val message: String
    ) : TaskListUiState()
}
