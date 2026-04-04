# SafeEar Android App - UI/UX Product Requirements Document

**Version**: 1.0  
**Date**: April 2, 2026  
**Framework**: Jetpack Compose with Material 3  
**Min SDK**: 26 | **Target SDK**: 35  
**Application**: Android Voice-Based Threat Detection

---

## 1. Executive Overview

### Purpose
SafeEar is a mobile threat detection app that uses speaker embedding technology to identify strangers/unknown speakers and alert parents. The app operates in two modes:
- **Parent Device**: Monitors alerts, manages speaker identities, reviews clips
- **Child Device**: Records audio in background, uploads chunks for real-time analysis

### Design Approach
- **Material 3 Design System** for consistency with Android 14+ ecosystem
- **Jetpack Compose** for modern reactive UI
- **Role-based Navigation** to present different UX per device type
- **Minimalist, Task-Focused Screens** for critical safety features
- **Haptic Feedback** for important user actions (swipe-to-dismiss, alerts)
- **Gradient Backgrounds** and soft color palette for safety-first aesthetic

---

## 2. Design System & Brand Identity

### Color Palette (Material 3)
```
Primary (Purple):      #6750A4  (light) / #D0BCFF (dark)
Secondary (Brown):     #625B71  (light) / #CCC2DC (dark)
Tertiary (Rose):       #7D5260  (light) / #EFBA8C (dark)
Error (Red):           #B3261E  (light) / #F2B8B5 (dark)
Success (Green):       #2B8A38  (safe zones, positive actions)
Warning (Orange):      #F57C00  (alerts, caution areas)
Background:            #FFFBFE  (light) / #1C1B1F (dark)
Surface:               #FFFBFE  (light) / #313033 (dark)
```

### Typography (Material 3)
```
Display Large:    56sp, light, +1.5% letter spacing
Display Medium:   45sp, light, 0% letter spacing
Display Small:    36sp, regular, 0% letter spacing
Headline Large:   32sp, regular, 0% letter spacing
Headline Medium:  28sp, regular, 0% letter spacing
Headline Small:   24sp, medium, 0% letter spacing
Title Large:      24sp, medium, 0% letter spacing
Title Medium:     16sp, medium, +0.15% letter spacing
Title Small:      14sp, medium, +0.1% letter spacing
Body Large:       16sp, regular, +0.5% letter spacing
Body Medium:      14sp, regular, +0.25% letter spacing
Body Small:       12sp, regular, +0.4% letter spacing
Label Large:      14sp, medium, +0.1% letter spacing
Label Medium:     12sp, medium, +0.5% letter spacing
Label Small:      11sp, medium, +0.5% letter spacing
```

### Spacing Scale
```
xs:  4dp
sm:  8dp
md:  12dp
lg:  16dp
xl:  24dp
2xl: 32dp
3xl: 48dp
```

### Border Radius
```
Small:   4dp
Medium:  12dp
Rounded: 24dp
```

### Elevation & Shadows
```
Surface:      0dp (no shadow)
Card:         2dp (subtle shadow)
Dialog:       24dp (prominent)
FAB:          6dp (raised)
```

---

## 3. Global Navigation Structure

### Navigation Model
- **Type**: Bottom Tab Navigation (Parent) + Stack Navigation (Child)
- **Initial Route**: Determined by `BuildConfig.DEBUG` + auth state + device role
- **Auth Flow**: DeviceToken + Google Sign-In OR Email/Password → Device Registration → Role-based Dashboard

### Flow Map

```
┌─ App Start
│  ├─ No Token? → LoginScreen
│  ├─ Token Valid? Processing...
│  ├─ Token Expired? → Auto-refresh or → LoginScreen
│  └─ Token Valid + No Device? → DeviceRegistrationScreen
│
├─ Parent Device Path
│  └─ ParentDashboard (Bottom Tabs)
│     ├─ Tab 1: Alerts (AlertsScreen)
│     ├─ Tab 2: Speakers (SpeakerListScreen)
│     ├─ Tab 3: Settings (SettingsScreen)
│     ├─ Stack: SpeakerEnrollmentScreen (from Tab 2)
│     └─ Stack: AlertDetailScreen (from Tab 1) [if implemented]
│
└─ Child Device Path
   └─ ChildMonitoringScreen (Full screen with toggle)
      └─ No tabs; Settings via floating action button or top menu
```

---

## 4. Shared UI Components

### 4.1 App Bar Patterns

#### Standard AppBar (Screens with back button)
```
┌────────────────────────────────────────┐
│ ← | Title                        ⋮ |
├────────────────────────────────────────┤
│ Screen Content                         │
└────────────────────────────────────────┘
```
- **Back button**: If in a nested stack (not home)
- **Title**: Screen name in Headline Small (Material 3)
- **Overflow menu**: Three dots if actions available
- **Height**: 64dp
- **Background**: Material 3 surface color

#### Top AppBar with Actions
```
┌────────────────────────────────────────┐
│        Title            [Action1] [Action2] |
├────────────────────────────────────────┤
```
- **Actions**: Icon buttons, right-aligned
- **Max 2 visible actions** (rest → overflow menu)

### 4.2 Buttons

#### Primary Button (Filled)
```
┌─────────────────────┐
│  Continue           │
└─────────────────────┘
```
- **Background**: Primary color (#6750A4)
- **Text**: White, Label Large
- **Height**: 40dp
- **Padding**: 16dp horizontal, 10dp vertical
- **Radius**: Medium (12dp)
- **State**: Disabled (50% opacity) when action unavailable
- **Interaction**: Material ripple effect

#### Secondary Button (Outlined)
```
┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─┐
│  Retry              │
└─ ─ ─ ─ ─ ─ ─ ─ ─ ─┘
```
- **Border**: 2dp Primary color
- **Text**: Primary color, Label Large
- **Height**: 40dp
- **Background**: Transparent → Primary on hover

#### Text Button (Minimal)
```
Forgot password?
```
- **Text**: Primary color, underlined
- **No border/background**
- **Height**: 40dp
- **Used for**: Links, skip actions

#### Icon Button
```
┌─────┐
│  ⋮  │
└─────┘
```
- **Size**: 48dp × 48dp
- **Icon**: 24dp
- **Background**: Transparent → Tertiary on hover

### 4.3 Input Fields

#### Text Input (Email, Password, Search)
```
┌─────────────────────────────────┐
│ Label                     [icon] │
├─────────────────────────────────┤
│ Placeholder or entered text     │
├─────────────────────────────────┘
```
- **Height**: 56dp
- **Border**: 1dp outline outline color, 2dp on focus (primary)
- **Radius**: Medium (12dp)
- **Label**: Body Small above field
- **Padding**: 12dp inside
- **Icon**: Optional (16dp) right-aligned
- **Error state**: Border red (#B3261E), error text below in red

#### Dropdown/Selector
```
┌─────────────────────┐
│ Select Role     ▼   │
├─────────────────────┤
│ Parent Device       │
│ Child Device        │
└─────────────────────┘
```
- **Height**: 56dp
- **Icon**: Dropdown chevron (24dp) right-aligned
- **Options**: Material dropdown menu below
- **Selected**: Checked icon + primary highlight

### 4.4 Cards

#### Standard Card
```
┌─────────────────────────────┐
│ ┌─────────────────────────┐ │
│ │ Speaker: John Doe       │ │
│ │ Status: Active          │ │
│ │ Enrolled: 3 days ago    │ │
│ └─────────────────────────┘ │
│ [Edit] [Delete]             │
└─────────────────────────────┘
```
- **Background**: Surface color with 2dp shadow
- **Radius**: Medium (12dp)
- **Padding**: 16dp
- **Elevation**: 2dp

#### Alert Card (High priority / warning)
```
┌─────────────────────────────┐
│ 🔔 UNKNOWN SPEAKER DETECTED │
│ Confidence: 94%             │
│ Time: 2:34 PM               │
│ [Play Clip] [Dismiss]       │
└─────────────────────────────┘
```
- **Background**: Warning color (#F57C00) with 20% opacity
- **Border**: 2dp Warning color
- **Icon**: Warning icon (24dp)
- **Radius**: Rounded (24dp)
- **Padding**: 16dp
- **Actions**: Buttons at bottom

### 4.5 Modals & Dialogs

#### Confirmation Dialog
```
┌───────────────────────────┐
│ Delete Speaker?           │
├───────────────────────────┤
│ This action cannot be     │
│ undone.                   │
├───────────────────────────┤
│ [Cancel] [Delete]         │
└───────────────────────────┘
```
- **Width**: 280dp
- **Padding**: 24dp
- **Title**: Headline Small
- **Content**: Body Medium
- **Actions**: 2 buttons at bottom, Cancel (outlined) + Primary (filled)
- **Background**: Scrim overlay (black 32%)
- **Radius**: Rounded (24dp)

#### Bottom Sheet (Action Menu)
```
┌─────────────────────────┐
│ ═ Handle bar           │
├─────────────────────────┤
│ • Edit                 │
│ • Rename               │
│ • Delete               │
└─────────────────────────┘
```
- **Slides from bottom**
- **Rounded riser (16dp) at top**
- **Full width, ~50% height max**
- **Items**: List with 48dp height each
- **Scrim**: Black 32% overlay

### 4.6 Snackbars & Toasts

#### Snackbar (Bottom message)
```
✓ Speaker deleted successfully    [Undo]
```
- **Position**: Bottom, 16dp from bottom
- **Background**: Inverse surface (#313033 light)
- **Text**: Inverse on surface (white)
- **Action**: Optional text button in primary color
- **Duration**: 3–5 seconds
- **Height**: 48dp + 16dp margin

#### Loading Indicator
```
╭─ ─ ─ ╮
─       ─       (Circular spinner)
╰─ ─ ─ ╯
```
- **Size**: 24dp (small), 48dp (large)
- **Color**: Primary gradient animation
- **Placement**: Center screen or inline

### 4.7 List Items

#### Standard List Item
```
┌─────────────────────────────┐
│ 👤 John Doe                 │
│    john@example.com         │
│    Enrolled 3 days ago  →   │
└─────────────────────────────┘
```
- **Height**: 56–72dp depending on content
- **Avatar**: 40dp circle, initial or icon
- **Title**: Body Large
- **Subtitle**: Body Medium, secondary color
- **Trailing**: Icon or badge
- **Divider**: 1px outline between items

#### Checkbox List Item
```
☐ John Doe (Speaker)
☑ Jane Doe (Speaker)
```
- **Checkbox**: 24dp, primary on checked
- **Height**: 48dp
- **Padding**: 16dp

---

## 5. Screen Specifications

### 5.1 LoginScreen

**Route**: `/login`  
**Accessible From**: App boot (if not authenticated)  
**Navigation Flow**: → DeviceRegistrationScreen (on success)

#### Purpose
Authenticate user via Google Sign-In or email/password combination. Support both dev token (for testing) and production OAuth.

#### Layout
```
┌──────────────────────────────────┐
│                                  │
│         SafeEar Logo             │
│       (or app icon)              │
│                                  │
│  ╭─── Login ──╮  ╭─ Sign Up ──╮ │
│  │            │  │            │ │
│  │ Email      │  │ Email      │ │
│  │ ___________│  │ ___________│ │
│  │            │  │            │ │
│  │ Password   │  │ Password   │ │
│  │ ___________│  │ ___________│ │
│  │            │  │            │ │
│  │            │  │ Full Name  │ │
│  │            │  │ ___________│ │
│  │            │  │            │ │
│  │ [Login]    │  │ [Sign Up]  │ │
│  │            │  │            │ │
│  │ Forgot?    │  │            │ │
│  ╰────────────╯  ╰────────────╯ │
│                                  │
│  ─── OR ───                      │
│                                  │
│  [🔵 Sign in with Google]        │
│                                  │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. Header Section**
- **Background**: Gradient (light purple #F4F6FF to light lavender #ECEFF7)
- **Logo/Icon**: 64dp, centered
- **Spacing**: 24dp top margin

**2. Tab Section (Material 3 TabRow)**
- **Tabs**: "Login" | "Sign Up"
- **Active tab indicator**: 3dp underline, primary color
- **Animation**: Smooth fade transition between tabs
- **Height**: 48dp

**3. Login Tab (Email Login)**
- **Email field**:
  - Label: "Email"
  - Hint: "you@example.com"
  - Validation: Must be valid email format (RFC 5322)
  - Icon: @ symbol (16dp)
  - Error display: "Invalid email format"

- **Password field**:
  - Label: "Password"
  - Masking: Dots until eye icon clicked
  - Eye icon: 24dp, toggle visibility
  - Validation: Minimum 8 characters
  - Error display: "Password must be 8+ characters"

- **Login Button**:
  - Label: "Login"
  - Style: Filled primary button (40dp height)
  - Width: Match parent minus 32dp padding
  - State: Disabled if email/password invalid
  - Loading: Show circular spinner inside button while authenticating

- **Forgot Password Link**:
  - Label: "Forgot password?"
  - Style: Text button, primary color
  - Action: Navigate to ForgotPasswordScreen (modal or stack)

**4. Sign Up Tab (Email Registration)**
- **Email field** (same as login)
- **Password field** (same as login, plus strength indicator):
  - Below field: "Strength: Weak / Fair / Strong" (color-coded)
  
- **Full Name field**:
  - Label: "Full Name"
  - Hint: "John Doe"
  - Validation: Non-empty, 3+ characters

- **Sign Up Button**:
  - Label: "Create Account"
  - Style: Filled primary button (40dp height)
  - State: Disabled if fields invalid
  - Loading: Show spinner while processing

**5. Divider Section**
- **Text**: "─ OR ─" centered, 16dp top/bottom margin

**6. Google Sign-In Button**
- **Label**: "🔵 Sign in with Google" or Google branded button
- **Background**: White or Google blue (#4285F4)
- **Height**: 48dp
- **Ripple**: Subtle Material ripple
- **Validation**: Only show if serverClientId is valid (ends with .apps.googleusercontent.com)
- **Error handling**: Show toast if ID token not returned or sign-in fails

**7. Error States**
- **Network error**: "Unable to connect. Check your internet."
- **Invalid credentials**: "Email or password is incorrect."
- **Account not found**: "No account found with this email."
- **Email already registered**: "This email is already registered."
- **Server error**: "Something went wrong. Please try again."
- **Retry button**: Appears on network/server errors

#### State Management
```kotlin
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val emailValidationError: String? = null,
    val passwordValidationError: String? = null,
    val displayNameValidationError: String? = null,
    val lastAction: String? = null  // email_login | email_register | google_signin
)
```

#### User Interactions
1. **Email input focus**: Border color changes from outline to primary
2. **Password visibility toggle**: Icon animates, text masking toggles instantly
3. **Tab switch**: Fade transition, form clears errors
4. **Login button tap**: Button shows loading spinner, touch disabled
5. **Google Sign-In tap**: Opens Google account chooser, shows toast on error
6. **Forgot password tap**: Navigate to new modal/screen

#### Validation Rules
- **Email**: Must match `android.util.Patterns.EMAIL_ADDRESS`
- **Password**: Minimum 8 characters
- **Full Name**: Non-empty, 3+ characters
- **Validation timing**: On field blur (not on every keystroke)
- **Error display**: Below field in red (error color)

#### Error Handling & Retry
- **Network errors**: Show snackbar with "Retry" button
- **Retry logic**: Keyed by `lastAction` (email_login, email_register, google_signin)
- **401 Unauthorized**: "Invalid credentials. Please try again."
- **403 Forbidden**: "Your account is not authorized."
- **422 Unprocessable**: "Invalid data. Please check your entries."

---

### 5.2 ForgotPasswordScreen

**Route**: `/forgot-password` (Modal)  
**Accessible From**: LoginScreen (Forgot password? link)  
**Navigation Flow**: → Continue after email submitted

#### Purpose
Allow users to initiate password reset by entering their email address.

#### Layout
```
┌──────────────────────────────────┐
│ ← | Forgot Password          ⋮   │
├──────────────────────────────────┤
│                                  │
│  Reset your password             │
│                                  │
│  We'll send you a link to        │
│  reset your password.            │
│                                  │
│  Email                           │
│  ___________________________      │
│                                  │
│                                  │
│  ✓ Code sent to your email       │
│                                  │
│  Enter 6-digit code:             │
│  ___ ___ ___ ___ ___ ___        │
│                                  │
│  [Didn't get it?] [→ More time?] │
│                                  │
│  New Password                    │
│  ___________________________      │
│                                  │
│  Confirm Password                │
│  ___________________________      │
│                                  │
│  [Reset Password]                │
│                                  │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. AppBar**
- **Back button**: Dismisses modal
- **Title**: "Forgot Password"

**2. Description Section**
- **Heading**: "Reset your password" (Headline Small)
- **Body**: "We'll send you a link to reset your password." (Body Medium)
- **Spacing**: 24dp below heading

**3. Email Input**
- Label: "Email"
- Validation: Same as LoginScreen
- Error: Show inline if invalid

**4. Verification State (After Email Submit)**
- **Status message**: "✓ Code sent to your email" (green, checkmark icon)
- **Code input**: 6 separate digit fields (OTP style)
  - Each field: 40×48dp
  - Spacing: 8dp between fields
  - Auto-advance to next field on digit entry
  - Auto-focus first field on screen load

- **Resend link**: "Didn't get it?" (text button)
  - Disabled for 60 seconds after send
  - Countdown: "Resend in 45s"

**5. Password Reset Section**
- **New Password field**:
  - Label: "New Password"
  - Masking with eye icon
  - Strength indicator below

- **Confirm Password field**:
  - Label: "Confirm Password"
  - Masking with eye icon
  - Validation: Must match "New Password"
  - Error: "Passwords do not match" (inline)

**6. Reset Button**
- Label: "Reset Password"
- Style: Filled primary (40dp)
- State: Disabled until all fields valid
- Loading: Show spinner

#### Error Handling
- **Email not found**: "No account found with this email."
- **Invalid code**: "Invalid or expired code. Please try again."
- **Code expired**: "Code expired. Request a new one."
- **Password mismatch**: "Passwords do not match."
- **Generic error**: "Something went wrong. Please try again."

---

### 5.3 DeviceRegistrationScreen

**Route**: `/device-registration`  
**Accessible From**: LoginScreen (on first login)  
**Navigation Flow**: → ParentDashboard or ChildMonitoringScreen (based on role selection)

#### Purpose
Simple, single-purpose screen to select device mode (parent or child) during initial app setup.

#### Layout
```
┌──────────────────────────────────┐
│                                  │
│     Select Device Mode           │
│                                  │
│  Choose how you'll use SafeEar   │
│                                  │
│                                  │
│  ◯ Parent Device                 │
│     Monitor alerts and manage    │
│     trusted speakers             │
│                                  │
│                                  │
│  ◯ Child Device                  │
│     Record and upload audio      │
│     for real-time analysis       │
│                                  │
│                                  │
│  [Continue]                      │
│                                  │
│  ⚠ Network Error                 │
│  Could not register device.      │
│  [Retry]                         │
│                                  │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. Header**
- **Title**: "Select Device Mode" (Headline Large, 32sp)
- **Subtitle**: "Choose how you'll use SafeEar" (Body Large)
- **Spacing**: 24dp below

**2. Radio Group (Material 3 RadioButton)**
- **Option 1 - Parent Device**:
  - Radio button (20dp) + Label text
  - Description: "Monitor alerts and manage trusted speakers" (Body Small, secondary color)
  - Full row touchable: 56dp height
  - Selected → Primary color + filled radio
  - Spacing: 16dp bottom margin

- **Option 2 - Child Device**:
  - Radio button (20dp) + Label text
  - Description: "Record and upload audio for real-time analysis" (Body Small, secondary color)
  - Full row touchable: 56dp height
  - Selected → Primary color + filled radio
  - Spacing: 24dp bottom margin

**3. Continue Button**
- Label: "Continue"
- Style: Filled primary button (40dp)
- Width: Match parent minus 32dp
- State: Disabled until role selected
- Loading: Show spinner; disable interaction

**4. Error State**
- **Error container**:
  - Background: Error color (#B3261E) @ 10% opacity
  - Border: 1dp error color
  - Radius: 12dp
  - Padding: 16dp
  - Icon: Warning icon (24dp)
  - Text: "Could not register device." (error color, Body Medium)
  - Sub-text: "Check your internet connection." (error color, Body Small)

- **Retry Button**:
  - Label: "Retry"
  - Style: Outlined secondary button (40dp)
  - Appears below error message
  - Clears previous error on tap

#### State Management
```kotlin
data class DeviceRegistrationUiState(
    val selectedRole: String? = null,  // "parent_device" | "child_device"
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRetryable: Boolean = false
)
```

#### Validation
- **Role selection**: Required before Continue enabled
- **Valid roles**: Exactly "parent_device" or "child_device"

#### Error Handling
- **Network error**: 401/403/422 → Friendly message + Retry button
- **Unknown error**: Generic message + Retry button
- **Success**: Navigate after 200ms delay for feedback

---

### 5.4 ParentDashboard (Tab Navigation Container)

**Route**: `/parent` (implicit root for parent devices)  
**Accessible From**: DeviceRegistrationScreen (role="parent_device")  
**Contains**: AlertsScreen, SpeakerListScreen, SettingsScreen

#### Purpose
Container for parent-focused features using bottom tab navigation.

#### Layout
```
┌──────────────────────────────────┐
│ ┌────────────────────────────────┤
│ │ [Alerts] | [Speakers] | [More] │
│ ├────────────────────────────────┤
│ │                                │
│ │  Alerts Content                │
│ │  (or other tab content)        │
│ │                                │
│ └────────────────────────────────┘
│                                  │
│ ┌────────────────────────────────┘
│ │  🔔 Alerts  🎤 Speakers  ⚙️ More
│ └────────────────────────────────┘
```

#### Components & Specifications

**1. Bottom Navigation Bar (Material 3)**
- **Style**: Filled navbar with labels + icons
- **Background**: Surface color
- **Elevation**: 8dp shadow
- **Height**: 80dp (56dp bar + 24dp system bottom bar on gesture nav)

**2. Tab Items (3 total)**
- **Tab 1: Alerts**
  - Icon: Bell/notification (24dp)
  - Label: "Alerts"
  - Ripple on press

- **Tab 2: Speakers**
  - Icon: Microphone/person (24dp)
  - Label: "Speakers"
  - Badge: Optional count (e.g., "3")

- **Tab 3: More (Settings)**
  - Icon: Gear/settings (24dp)
  - Label: "Settings"

**3. Tab Content Area**
- Full screen above navbar
- Safe area respects system insets

#### Navigation Rules
- **Switching tabs**: Smooth crossfade (200ms)
- **Preserving state**: Each tab maintains separate state (retained by Compose nav)
- **Back button**: Only active in nested stacks; pressing back closes nested screen first

---

### 5.5 AlertsScreen

**Route**: `/parent/alerts`  
**Accessible From**: ParentDashboard (Alerts tab)  
**Navigation Flow**: Can push to AlertDetailScreen (if implemented) or elsewhere

#### Purpose
Display real-time alerts when unknown speakers are detected. Show audio clips, confidence scores, and allow acknowledge/dismiss actions.

#### Layout
```
┌──────────────────────────────────┐
│ 🔔 | Alerts                  ⋮   │
├──────────────────────────────────┤
│ ┌──────────────────────────────┐ │
│ │ ← Swipe to dismiss →         │ │
│ │ 🚨 UNKNOWN SPEAKER           │ │
│ │    Confidence: 94%           │ │
│ │    Time: 2:34 PM             │ │
│ │    [🔊 Play] [✓ Ack]        │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌──────────────────────────────┐ │
│ │ ← Swipe to dismiss →         │ │
│ │ 🚨 UNKNOWN SPEAKER           │ │
│ │    Confidence: 87%           │ │
│ │    Time: 1:12 PM             │ │
│ │    [🔊 Play] [✓ Ack]        │ │
│ └──────────────────────────────┘ │
│                                  │
│           [Clear All]            │
│                                  │
├──────────────────────────────────┤
│ 🔔 Alerts  🎤 Speakers ⚙️ More    │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. AppBar**
- **Title**: "Alerts" (Headline Small)
- **Overflow menu**: Three dots (if more actions)
- **Badge**: Optional count of unread alerts

**2. Empty State** (if no alerts)
```
              🔔
        No Alerts Yet
    
    Unknown speakers detected
    will appear here in real-time.
```
- Icon: Large bell (48dp), secondary color
- Title: "No Alerts Yet" (Headline Small)
- Subtitle: Multi-line message (Body Medium)
- Centered, padding 48dp top/bottom

**3. Alert List (Material 3 LazyColumn)**
- **Animation**: animateItem() for smooth layout changes (Material 3 extension)
- **Item height**: 120–140dp per alert card

**4. Individual Alert Card (Material 3 SwipeToDismissBox)**
- **Background when swiped right**: Red (#B3261E)
- **Background when swiped left**: Green (#2B8A38)
- **Background in idle state**: Transparent (no background visible)
- **Card at front**: Surface color, 2dp shadow, rounded corners (12dp)
- **Swipe zones**:
  - Swipe right (forward): Trigger dismiss/delete
  - Swipe left (back): Trigger acknowledge
- **Haptic feedback**: HapticFeedbackType.LongPress on swipe threshold (75% progress)
- **Dismissal animation**: 200ms fade-out after action

**5. Alert Card Content (Front Layer)**
- **Icon**: Warning/alert icon (24dp), warning color (#F57C00)
- **Title**: "UNKNOWN SPEAKER" (Title Medium, bold)
- **Confidence**: "Confidence: 94%" (Body Small, muted)
- **Timestamp**: "2:34 PM" (Body Small, muted)
- **Actions** (if not swiped):
  - Play button: "🔊 Play" (icon + text, 32dp height)
  - Acknowledge button: "✓ Ack" (icon + text, 32dp height)
  - Buttons: Outlined style, 8dp spacing

**6. Clear All Button** (if alerts present)
- Appears below list at bottom
- Label: "Clear All"
- Style: Text button (minimal)
- Action: Shows confirmation dialog

**7. Confirmation Dialog** (for Clear All)
- Title: "Clear all alerts?"
- Body: "This action cannot be undone."
- Buttons: Cancel (outlined) | Clear (filled, error color)

#### State Management
```kotlin
data class AlertsUiState(
    val alerts: List<Alert> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val refreshing: Boolean = false
)

data class Alert(
    val id: String,
    val confidence: Float,  // 0-100
    val timestamp: LocalDateTime,
    val clipUrl: String?,
    val isAcknowledged: Boolean
)
```

#### User Interactions
1. **Swipe right**: Delete alert (background fades red)
2. **Swipe left**: Acknowledge (background fades green)
3. **Tap Play**: Download and play audio clip in ExoPlayer
4. **Tap Ack**: Mark as acknowledged (remove from list)
5. **Clear All**: Confirm dialog, then clear all at once
6. **Pull to refresh**: Trigger new polling cycle (15s interval)

#### Loading & Error States
- **Initial load**: Show skeleton loaders (3× placeholder cards)
- **Refresh error**: Snackbar "Failed to refresh. [Retry]" — tapping Retry calls alertsViewModel.refresh()
- **Playback error**: Toast "Could not play audio"
- **Polling**: Automatic every 15s in background; stop when screen paused

---

### 5.6 SpeakerListScreen

**Route**: `/parent/speakers`  
**Accessible From**: ParentDashboard (Speakers tab)  
**Navigation Flow**: Can push to SpeakerEnrollmentScreen or SpeakerDetailScreen

#### Purpose
Manage enrolled trusted speakers: list, enroll new, rename, delete.

#### Layout
```
┌──────────────────────────────────┐
│ 🎤 | My Speakers             [+] │
├──────────────────────────────────┤
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 👤 John Doe                  │ │
│ │    john@example.com          │ │
│ │    Enrolled 3 days ago   ⋯   │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 👤 Jane Smith                │ │
│ │    jane@example.com          │ │
│ │    Enrolled 7 days ago   ⋯   │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 👤 Bob Johnson               │ │
│ │    bob@example.com           │ │
│ │    Enrolled 21 days ago  ⋯   │ │
│ └──────────────────────────────┘ │
│                                  │
│           No More Speakers       │
│                                  │
├──────────────────────────────────┤
│ 🔔 Alerts  🎤 Speakers ⚙️ More    │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. AppBar**
- **Title**: "My Speakers" (Headline Small)
- **Action Button**: "+" (FAB-style icon button, 40dp)
  - Action: Navigate to SpeakerEnrollmentScreen
  - Tooltip: "Enroll Speaker" (on long press)

**2. Empty State** (if no speakers)
```
              🎤
       No Speakers Enrolled
    
    Add trusted speakers to get started.
    Unknown speakers will trigger alerts.
    
           [Enroll Speaker]
```
- Icon: Microphone (48dp), secondary color
- Title: "No Speakers Enrolled" (Headline Small)
- Subtitle: Multi-line message (Body Medium)
- CTA Button: "Enroll Speaker" (Filled primary)

**3. Speaker List (Material 3 LazyColumn)**
- **Item height**: 64–72dp per speaker
- **Animation**: animateItem() for smooth entry/exit

**4. Speaker List Item (Card with Actions)**
```
┌─────────────────────────────────┐
│ 👤 John Doe                 [⋯] │
│    john@example.com             │
│    Enrolled 3 days ago          │
└─────────────────────────────────┘
```

- **Avatar**: 40dp circle, initials ("JD") in primary color
- **Title**: "John Doe" (Body Large)
- **Email**: "john@example.com" (Body Small, secondary color)
- **Meta**: "Enrolled 3 days ago" (Body Small, muted)
- **Overflow button**: Three dots (24dp), opens bottom sheet

**5. Bottom Sheet (Action Menu)**
```
┌─────────────────────────────┐
│ ═ Handle bar               │
├─────────────────────────────┤
│ • Rename                    │
│ • Delete                    │
└─────────────────────────────┘
```
- Items: 48dp height each
- Icon + label, left-aligned
- Tap to close sheet after action

**6. Rename Dialog**
```
┌────────────────────────────┐
│ Rename Speaker             │
├────────────────────────────┤
│ New Name                   │
│ ___________________        │
│                            │
│ [Cancel] [Save]            │
└────────────────────────────┘
```
- Text field with current name pre-filled
- Save button enabled only if text changed
- Validation: Non-empty, 3+ characters

**7. Delete Confirmation Dialog**
```
┌────────────────────────────┐
│ Delete Speaker?            │
├────────────────────────────┤
│ John Doe will be removed   │
│ from trusted speakers.     │
│                            │
│ [Cancel] [Delete]          │
└────────────────────────────┘
```
- Title: "Delete Speaker?"
- Body: Confirmation text
- Delete button: Error color (#B3261E)

#### State Management
```kotlin
data class SpeakerListUiState(
    val speakers: List<Speaker> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class Speaker(
    val speakerId: String,
    val displayName: String,
    val enrolledAt: LocalDateTime
)
```

#### User Interactions
1. **Tap "+" FAB**: Navigate to SpeakerEnrollmentScreen
2. **Tap overflow menu**: Show bottom sheet
3. **Select "Rename"**: Show rename dialog; update on save
4. **Select "Delete"**: Show confirmation; delete on confirm
5. **Pull to refresh**: Fetch latest speaker list (reload)
6. **Tap speaker card**: (Optional) Navigate to detail view

---

### 5.7 SpeakerEnrollmentScreen

**Route**: `/parent/speakers/enroll`  
**Accessible From**: SpeakerListScreen (+ FAB)  
**Navigation Flow**: → SpeakerListScreen (on success) or stay (on error)

#### Purpose
Record or upload audio of a new speaker to enroll them as trusted and generate embeddings.

#### Layout
```
┌──────────────────────────────────┐
│ ← | Enroll Speaker           ⋮   │
├──────────────────────────────────┤
│                                  │
│  Declare Speaker                 │
│                                  │
│  Speaker Name                    │
│  ___________________________      │
│                                  │
│                                  │
│     [🎤 Record Audio]            │
│                                  │
│         - OR -                   │
│                                  │
│     [📁 Choose File]             │
│                                  │
│                                  │
│  Feedback:                       │
│  ┌──────────────────────────┐    │
│  │ No audio selected.       │    │
│  └──────────────────────────┘    │
│                                  │
│  [Enroll]                        │
│                                  │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. AppBar**
- **Back button**: Pop screen
- **Title**: "Enroll Speaker"

**2. Form Section**

**Speaker Name Input**
- Label: "Speaker Name"
- Placeholder: "e.g., John, Mom, Dad"
- Validation: Non-empty, 3+ characters
- Error display: Below field

**Audio Selection (Mutually Exclusive)**

- **Option A: Record Audio Button**
  - Icon: Microphone (24dp) + waveform animation (while recording)
  - Label: "🎤 Record Audio" or "🎤 Recording..." (during capture)
  - Style: Filled primary (48dp height)
  - On tap: Show recording UI (see below)

- **Option B: Choose File Button**
  - Icon: Folder (24dp)
  - Label: "📁 Choose File"
  - Style: Outlined secondary (48dp height)
  - On tap: Open file picker (audio files only)
  - Validation: File must be WAV/MP3, <10s recommended

**Recording UI (Modal overlay)**
```
┌──────────────────────────────────┐
│              ◯ Recording...       │
│              [Stop] [Cancel]      │
│         ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▆ ▅     │
│            (Waveform)            │
│         Duration: 0:03           │
└──────────────────────────────────┘
```
- Centered modal with recording circle (pulse animation)
- Live waveform visualization (bars animating to audio levels)
- Duration counter: "0:MM:SS" (seconds only initially)
- Stop button: Outlined, stops recording
- Cancel button: Discards recording
- Close on tap outside: Cancels

**3. Feedback Section**
- **Container**: Background light gray (#F5F5F5), rounded (12dp), padding 16dp
- **Content**: Real-time feedback from backend
  - "No audio selected." (initial)
  - "📤 Uploading audio..." (during upload)
  - "✓ Embedding generated!" (success)
  - "❌ Could not process audio. Try again." (error)
- **Icon**: Status icon (24dp)
- **Text**: Body Medium

**4. Enroll Button**
- Label: "Enroll"
- Style: Filled primary (40dp)
- State: Disabled until name + audio selected
- Loading: Show spinner during upload/embedding generation

#### State Management
```kotlin
data class SpeakerEnrollmentUiState(
    val speakerName: String = "",
    val audioFile: AudioFile? = null,
    val isRecording: Boolean = false,
    val recordingDuration: Duration = Duration.ZERO,
    val feedback: String = "No audio selected.",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

data class AudioFile(
    val fileName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val data: ByteArray
)
```

#### User Interactions
1. **Name input**: Real-time validation, error on blur if invalid
2. **Record button**: Toggle recording on/off; show waveform during capture
3. **Stop recording**: Audio is processed, waveform freezes
4. **File picker**: Select audio file; show filename + duration
5. **Re-record**: Clear previous audio; start new recording
6. **Enroll**: Upload multipart form (display_name, file, optional speaker_id); show feedback; on success → pop screen

#### Validation
- **Name**: 3+ characters, non-empty
- **Audio**: Present, <30s duration recommended
- **Format**: WAV or MP3 accepted

#### Error Handling
- **Network error**: "Could not upload. [Retry]"
- **Invalid audio**: "Audio format not supported."
- **Server error**: "Enrollment failed. Please try again."

---

### 5.8 ChildMonitoringScreen

**Route**: `/child` (implicit root for child devices)  
**Accessible From**: DeviceRegistrationScreen (role="child_device")  
**Navigation Flow**: Full-screen monitoring; Settings access via menu/FAB

#### Purpose
Primary interface for child device: large toggle to start/stop recording, display real-time detection results and alerts fired.

#### Layout
```
┌──────────────────────────────────┐
│        Safe Mode Active          │
│                                  │
│                                  │
│      ┌─────────────────┐        │
│      │                 │        │
│      │   ◯ Recording   │        │
│      │                 │        │
│      └─────────────────┘        │
│                                  │
│                                  │
│  Status:                         │
│  Decision: Stranger Detected     │
│  Confidence: 94%                 │
│  Last Updated: 2:34 PM           │
│                                  │
│                                  │
│  📢 Alerts This Session:         │
│  • Unknown speaker 2x            │
│  • Baby cry 1x                   │
│                                  │
│                                  │
│                         [⚙️ ⋮]  │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. Header / Status Bar**
- **Status text**: "Safe Mode Active" or "Monitoring Paused" (Title Large, primary color)
- **Background**: Gradient (light green or neutral)
- **Padding**: 16dp

**2. Large Recording Toggle (Circular FAB)**
- **Size**: 120dp diameter (or fill 60% of screen width, whichever fits)
- **Shape**: Perfect circle
- **Background**: Material 3 filled button (primary color when inactive, success green when recording)
- **Icon**: Microphone (48dp) or Stop square during recording
- **Animation**: Pulse effect when recording (scale 1.0 → 1.1 → 1.0, 1s loop)
- **Text below**: "▯ Recording" (during record) or "◉ Tap to Start" (during idle)
- **Centered** on screen, 40% from top
- **Tap**: Toggle recording on/off

**3. Status Section** (Below toggle)
- **Container**: Subtle card (1dp outline, rounded 12dp), padding 16dp
- **Items**:
  - **Decision**: "Stranger Detected" (Body Large, semi-bold)
  - **Confidence**: "94%" (Body Medium, secondary)
  - **Timestamp**: "Last Updated: 2:34 PM" (Body Small, muted)
- **Animation**: Fade in/out when decision changes

**4. Alerts Summary** (Below status)
- **Title**: "📢 Alerts This Session:" (Title Small)
- **List**:
  - "Unknown speaker 2x" (Body Medium)
  - "Baby cry 1x" (Body Medium)
  - ... (dynamic items)
- **Hidden if no alerts**

**5. Stopping State Overlay** (During stop operation)
```
┌──────────────────────────────────┐
│                                  │
│         Stopping...              │
│                                  │
│     ╭─ ─ ─ ╮                    │
│     ─       ─ (spinner)          │
│     ╰─ ─ ─ ╯                    │
│                                  │
│  Please wait while we             │
│  finalize your session.          │
│                                  │
└──────────────────────────────────┘
```
- Overlay (scrim black 32%)
- Centered content with spinner
- Message text (Body Medium)
- **Stop button disabled** until operation completes

**6. Bottom Action Menu (Three-dot or Settings Fab)**
- **Icon button**: 48dp, three dots or gear icon
- **Placement**: Bottom-right corner, 16dp from edges
- **On tap**: Show bottom sheet menu

**7. Bottom Sheet Menu**
```
┌─────────────────────────────┐
│ ═ Handle bar               │
├─────────────────────────────┤
│ • Device Info              │
│ • Settings                 │
│ • Logout                   │
└─────────────────────────────┘
```

#### State Management
```kotlin
data class ChildMonitoringUiState(
    val isRecording: Boolean = false,
    val isStopping: Boolean = false,
    val lastDecision: String? = null,  // "Stranger Detected", "Known Speaker", etc.
    val confidence: Float? = null,      // 0-100
    val lastUpdatedAt: LocalDateTime? = null,
    val alertsSummary: List<String> = emptyList(),
    val error: String? = null
)
```

#### User Interactions
1. **Tap toggle (Idle)**: Start recording; button animates to green, pulse effect starts
2. **Tap toggle (Recording)**: Stop recording; show "Stopping..." overlay, disable button
3. **Stopping completes**: Overlay fades, button returns to inactive state
4. **Stop error**: Show snackbar with retry option
5. **Tap settings FAB**: Show bottom sheet menu
6. **Select "Logout"**: Confirm dialog, then logout

#### Key Features
- **Auto-update decision**: Real-time detection results from backend polling
- **Haptic feedback**: Strong vibration on toggle tap (both start/stop)
- **Prevent mistouch**: Stop button disabled during isStopping
- **Graceful degradation**: If API fails, show generic error; allow user to retry stop

---

### 5.9 SettingsScreen

**Route**: `/parent/settings` or `/settings` (child-accessible via menu)  
**Accessible From**: ParentDashboard (Settings tab) or ChildMonitoringScreen (menu)  
**Navigation Flow**: Can navigate to LoginScreen (on logout)

#### Purpose
Display device info, app version, and provide logout option.

#### Layout (Parent Device View)
```
┌──────────────────────────────────┐
│ ⚙️ | Settings                ⋮   │
├──────────────────────────────────┤
│                                  │
│ Device Information               │
│ ┌──────────────────────────────┐ │
│ │ Device Name                  │ │
│ │ My iPhone                    │ │
│ │                              │ │
│ │ Device ID                    │ │
│ │ abc-123-def-456              │ │
│ │                              │ │
│ │ Device Mode                  │ │
│ │ Parent Device                │ │
│ │                              │ │
│ │ App Version                  │ │
│ │ 1.0.0 (Build 42)             │ │
│ └──────────────────────────────┘ │
│                                  │
│ Account                          │
│ ┌──────────────────────────────┐ │
│ │ Email                        │ │
│ │ user@example.com             │ │
│ │                              │ │
│ │ Last Sync                    │ │
│ │ 2 minutes ago                │ │
│ └──────────────────────────────┘ │
│                                  │
│                                  │
│ [Logout]                         │
│                                  │
├──────────────────────────────────┤
│ 🔔 Alerts  🎤 Speakers ⚙️ More    │
└──────────────────────────────────┘
```

#### Components & Specifications

**1. AppBar**
- **Title**: "Settings" (Headline Small)
- **Overflow menu**: Optional

**2. Settings Sections (Material 3 Cards)**

**Section 1: Device Information**
- **Title**: "Device Information" (Headline Small, padding 16dp top)
- **Items**:
  - **Field**: "Device Name" → Value: "My iPhone" (read-only)
  - **Field**: "Device ID" → Value: "abc-123-def-456" (monospace, copyable on long press)
  - **Field**: "Device Mode" → Value: "Parent Device" (read-only)
  - **Field**: "App Version" → Value: "1.0.0 (Build 42)" (read-only)
- **Format**: Key-value list in card, 12dp padding
- **Dividers**: 1px between items

**Section 2: Account**
- **Title**: "Account" (Headline Small, padding 16dp top)
- **Items**:
  - **Field**: "Email" → Value: "user@example.com" (read-only)
  - **Field**: "Last Sync" → Value: "2 minutes ago" (relative time, updates every second)
- **Format**: Key-value list in card, 12dp padding

**3. Logout Button**
- Label: "Logout"
- Style: Filled error color (#B3261E)
- Height: 40dp
- Width: Match parent minus 32dp padding
- Placement: Bottom of screen, 16dp margin
- On tap: Show confirmation dialog

**4. Logout Confirmation Dialog**
```
┌────────────────────────────┐
│ Logout?                    │
├────────────────────────────┤
│ Are you sure? You'll need  │
│ to log in again.           │
│                            │
│ [Cancel] [Logout]          │
└────────────────────────────┘
```
- Title: "Logout?"
- Body: Confirmation text
- Cancel button: Outlined
- Logout button: Filled error color

#### State Management
```kotlin
data class SettingsUiState(
    val deviceName: String = "",
    val deviceId: String = "",
    val deviceMode: String = "",
    val appVersion: String = "",
    val email: String = "",
    val lastSyncTime: LocalDateTime? = null,
    val isLoggingOut: Boolean = false,
    val error: String? = null
)
```

#### User Interactions
1. **Long press device ID**: Copy to clipboard, show toast "Copied"
2. **Last Sync time**: Updates in real-time (or on screen focus)
3. **Logout button**: Show confirmation dialog
4. **Confirm logout**: Call logout API, show loading state, then navigate to LoginScreen

#### Error Handling
- **Logout error**: "Logout failed. [Retry]" snackbar
- **Retry**: Calls logout again
- **On logout success**: Clear tokens, navigate to LoginScreen

---

## 6. Animation & Micro-interactions

### Standard Animations

**Transition Between Screens**
- Duration: 200–300ms
- Easing: EaseInOut (Material Curves.FastOutSlowIn)
- Type: Crossfade (opacity fade) or Slide (horizontal entry/exit)

**Button Press Feedback**
- Material ripple effect (200ms)
- No scale or color shift (ripple is enough)

**Loading Spinners**
- Duration: 1.5s per rotation loop
- Color: Material 3 primary color
- Easing: Linear (continuous rotation)

**List Item Entry/Exit**
- Enter: Fade + translate from top (100dp)
- Duration: 300ms
- Easing: EaseOut
- animateItem() for list reorders

**Modal Entry**
- Duration: 250ms
- Type: Scale (0.9x → 1.0x) + Fade
- Easing: Linear
- Scrim: Fade in simultaneously

**Toggle/Checkbox State Change**
- Duration: 150ms
- Type: Scale (1.0x → 1.2x → 1.0x) and color fade
- Easing: EaseInOut

### Haptic Feedback

**Long Press / Confirm Action**
- Type: HapticFeedbackType.LongPress
- Used on: Swipe-to-dismiss alert (75% threshold), button confirms

**Tap / Light Feedback**
- Type: HapticFeedbackType.Error or Light tap
- Used on: Toggle tap, menu open

**Success / Completion**
- Type: Double tap or success pattern
- Used on: Enrollment success, logout confirmation

---

## 7. Error Handling & User Feedback

### Error Display Patterns

**Network Errors**
```
┌──────────────────────────────┐
│ ⚠ No Internet Connection     │
│ Please check your connection │
│ and try again.               │
│                              │
│ [Retry] [Dismiss]            │
└──────────────────────────────┘
```
- Snackbar: 5s duration with Retry button
- Icon: Warning (24dp)
- Background: Error color @ 20% opacity
- Text: error color

**Server Errors (5xx)**
```
⚠ Something went wrong
Please try again later.
[Retry]
```
- Toast or snackbar
- 5s duration
- Retry button available

**Validation Errors**
```
Email field:
─────────────────────────
[Email address]         ✗

Invalid email format | Error in red below field
```
- Inline error text below field
- Field border turns red (2dp)
- Error icon (small, 16dp)
- Clears on next keystroke

**No Results**
```
            🔎
       No Results

  Your search returned no speakers.
  Try different keywords.
```
- Large icon (48dp)
- Title: Headline Small
- Subtitle: Body Medium
- Centered on screen

---

## 8. Mobile-Specific Considerations

### Touch Targets
- Minimum 48×48dp for all interactive elements
- Buttons: 40–56dp height
- Icon buttons: 48×48dp
- Spacing: 8dp minimum between adjacent targets

### Keyboard Handling
- **Text inputs**: Show email keyboard (inputType emailAddress)
- **Password inputs**: Show password keyboard (inputType password)
- **Number inputs**: Show numeric keyboard (inputType number)
- **On submit**: Call primary action (login, enroll, etc.)
- **Keyboard dismiss**: On scroll or back press

### Safearea/Inset Handling
- **Padding**: Content respects system insets (notches, gesture bars)
- **BottomSheet**: Slides above gesture bar (consider 24dp clearance)
- **FAB**: 16dp from bottom edge + gesture bar height

### Landscape Mode
- **Top-level screens**: Support landscape (ParentDashboard, ChildMonitoringScreen)
- **Modals**: Full width or constrain to max 600dp
- **Text-heavy screens**: Use scrollable containers

### Accessibility
- **Contrast**: Text/background >= 4.5:1 (WCAG AA)
- **Touch targets**: All 48×48dp minimum
- **Labels**: All icons have contentDescription (Compose Modifier.semantics)
- **Focus**: Keyboard navigation via tab/arrow keys
- **TalkBack**: Supported (no custom gestures)

---

## 9. State Management & Data Flow

### Global App State
```kotlin
sealed class AppState {
    object Splash : AppState()       // Loading auth + device state
    object Login : AppState()        // User on LoginScreen
    object DeviceRegistration : AppState()  // On DeviceRegistrationScreen
    data class ParentDashboard(val alerts: List<Alert>) : AppState()
    data class ChildMonitoring(val isRecording: Boolean) : AppState()
    object Error : AppState()        // Fatal error state
}
```

### Screen-Level State
- **HomeScreenState**: isLoading, error, isLoggedIn, etc.
- **Managed via ViewModel + MutableStateFlow**
- **Collected via collectAsState() in Composable**
- **Updates trigger recomposition**

### Navigation State
- **Current route**: Tracked by Compose NavController
- **Back stack**: Preserved automatically
- **Deep linking**: Supported for share/shortcuts

---

## 10. Responsive Design Guidelines

### Screen Sizes
- **Compact**: < 600dp (phones in portrait)
- **Medium**: 600–840dp (tablets, foldables)
- **Expanded**: > 840dp (tablets, large screens)

### Adaptation Strategy
**Compact (Current focus)**
- Full width, bottom tabs, modal dialogs

**Medium**
- Consider: Side navigation drawer or split-view
- Top nav tabs OR side-drawer
- Width-constrain content to ~600dp max

**Expanded**
- Split-view layout (list left, detail right)
- Multi-pane navigation
- Content pane width: 800dp max

---

## 11. Testing Checklist (for Stitch AI Implementation)

### Visual Regression Tests
- [ ] LoginScreen renders with all tabs, buttons, inputs
- [ ] DeviceRegistrationScreen radio buttons render correctly
- [ ] AlertsScreen list items swipe-dismiss (red/green backgrounds on swipe)
- [ ] SpeakerListScreen empty state renders correctly
- [ ] ParentDashboard bottom tabs render + switch smoothly
- [ ] ChildMonitoringScreen toggle renders large and centered
- [ ] SettingsScreen card layout matches spec

### Interaction Tests
- [ ] Buttons react to tap (ripple effect, animation)
- [ ] Text inputs accept text, show validation errors
- [ ] Swipe-to-dismiss on alerts fires haptic feedback
- [ ] Tab switches show crossfade transition
- [ ] Modals open/close with scale + fade animation
- [ ] FAB expands/collapses correctly

### State Tests
- [ ] Loading state shows spinner
- [ ] Error state displays message + retry button
- [ ] Empty state renders when no data
- [ ] Active/selected tab highlighted in navbar
- [ ] Recording toggle animates (pulse, color change)

### Accessibility Tests
- [ ] All icons have contentDescription
- [ ] Text contrast >= 4.5:1
- [ ] Touch targets >= 48×48dp
- [ ] Keyboard navigation works
- [ ] Screen reader (TalkBack) reads content

---

## 12. Implementation Notes for Stitch AI

### Key Libraries & Versions
```
Jetpack Compose: 2024.11.00
Material 3: androidx.compose.material3:material3:1.3.0
Navigation: androidx.navigation:navigation-compose:2.8.x
Lifecycle: androidx.compose.runtime:runtime:1.x
Coroutines: org.jetbrains.kotlinx:kotlinx-coroutines:1.8.1
```

### Custom Composables to Implement
1. **SwipeToDismissBox** (Material 3 extension for alerts)
2. **AnimatedTabRow** (for smooth tab transitions)
3. **CircularToggle** (large 120dp toggle button)
4. **RecordingWaveform** (live audio visualization)
5. **StatusCard** (reusable for alerts, status)

### ViewModel Integration Points
- Inject ViewModel via hiltViewModel() in screens
- Subscribe to uiState.collectAsState()
- Call viewModel methods on user action (button tap, input change, etc.)
- Screen handles loading/error display based on UiState

### Navigation Integration
- Use `NavController.navigate(route)` for manual navigation
- Routes defined in sealed class Screen
- Pass callbacks via composable parameters for navigation
- Pop back stack: `navController.popBackStack()`

### Theming
- Use Material 3 colors from Color.kt
- Apply Typography from Type.kt
- Dark/light mode auto-handled by Android system
- No custom color overrides needed (stick to Material 3 palette)

---

## Appendix: Color Reference

### Usage Guidelines
| Element | Color | Notes |
|---------|-------|-------|
| Primary buttons | #6750A4 | Filled, active states |
| Secondary buttons | #625B71 | Outlined, less prominent |
| Success / Positive | #2B8A38 | Checkmarks, done states |
| Warning / Caution | #F57C00 | Alerts, important messages |
| Error / Destructive | #B3261E | Delete, logout, errors |
| Dividers | #E7E0EC | 1px separators |
| Text primary | #1C1B1F | Body text, headings |
| Text secondary | #49454F | Hints, captions |
| Background | #FFFBFE | Page background |
| Surface | #FFFBFE | Cards, inputs |

---

**Document Version**: 1.0  
**Last Updated**: April 2, 2026  
**Framework**: Jetpack Compose + Material 3  
**Target Audience**: UI Designers, AI Code Generators (Stitch AI), Frontend Developers
