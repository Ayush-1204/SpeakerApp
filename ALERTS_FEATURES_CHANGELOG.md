# Alerts Section - Clear All & Swipe-to-Delete Implementation

## Features Added

### 1. Clear All Button
- Added a "Clear All" icon button in the top-right of the Alerts screen (TopAppBar)
- Shows a confirmation dialog before clearing all alerts
- Only visible when there are alerts to clear
- Calls `deleteAllAlerts()` API endpoint to remove all alerts at once

### 2. Swipe-to-Delete
- Swipe left or right on any alert card to reveal a red delete button
- Drag threshold: 100 pixels to trigger deletion
- Smooth drag animation with resistance (10:1 ratio)
- Delete button appears on the right side with red background
- Works both left and right swipe directions
- Resets to center when swiped less than threshold amount
- Includes delete state UI feedback during deletion

## Code Changes

### New API Endpoints (ApiService.kt)
```kotlin
@DELETE("alerts/{alert_id}")
suspend fun deleteAlert(@Path("alert_id") alertId: String): Response<Unit>

@DELETE("alerts")
suspend fun deleteAllAlerts(): Response<Unit>
```

### Repository Methods (AlertsRepository.kt)
- `deleteAlert(alertId)` - Delete specific alert
- `deleteAllAlerts()` - Delete all alerts

### ViewModel Methods (AlertsViewModel.kt)
- `deleteAlert(alertId)` - Handle UI state for single alert deletion
- `deleteAllAlerts()` - Handle UI state for clearing all alerts
- Updated `AlertsUiState` with `deletingAlertId` and `isDeletingAll` flags

### UI Updates (AlertsScreen.kt)

**AlertsScreen:**
- Added "Clear All" icon button (ClearAll icon) in TopAppBar
- Added confirmation dialog for clearing all alerts
- Shows alert count in confirmation: "Clear All {N} alerts?"

**AlertCard:**
- Added `swipeOffset` state to track drag position
- Added `cardHeight` state for responsive delete button sizing
- Implemented `detectHorizontalDragGestures` for swipe detection
- Red delete background appears when swiped >20 pixels
- Snaps to delete action at 100+ pixel threshold
- Drag resistance applied (1/10 ratio for smooth UX)
- Delete button positioned on right edge with white icon
- Disabled other buttons during deletion state (`isDeleting` parameter)

## Imports Added
- `detectHorizontalDragGestures` - For swipe detection
- `background` - For red delete background
- `ClearAll` icon - For clear all button
- `pointerInput` - For touch input handling
- `onGloballyPositioned` - For card height measurement

## User Experience

**To Clear All Alerts:**
1. Tap the "ClearAll" icon in top-right corner
2. Confirm in dialog
3. All alerts deleted

**To Delete Single Alert:**
1. Swipe left or right on any alert card
2. A red delete button appears
3. Continue dragging to threshold (100px worth)
4. Alert auto-deletes when released
5. OR tap the red delete button to immediately delete

## API Contract
Both endpoints expect the backend to support:
- `DELETE /alerts/{alert_id}` - Delete specific alert by ID
- `DELETE /alerts` - Delete all alerts for current user

If backend doesn't support these, the methods will fail gracefully with error messages.

## State Management
- Deletion state tracked per alert (`deletingAlertId`)
- Mass deletion state tracked (`isDeletingAll`)
- Disables UI interactions during deletion to prevent race conditions
- Auto-removes deleted alerts from local list
- Shows error message if deletion fails
