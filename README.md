# אברהם העברי - אנדרואיד

אפליקציית Android אופליין מבוססת מילות מפתח וטריגרים. ללא הרשאת INTERNET וללא שירות AI/שרת.

### מובנה באפליקציה
- 4,000 שמות אפליקציות בעברית ובאנגלית.
- 4,000 תבניות פעולות מערכת והגדרות בעברית ובאנגלית.
- 5,000 רשומות תגובה מוגדרות מראש.
- 6,000 רשומות מילים נרדפות וטריגרים.
- זיהוי פעולה לפי מצב מפורש או פעלי פתיחה/פעולה.
- תיקון שגיאות כתיב באמצעות Levenshtein עד 2 בעת התאמת אפליקציות.
- fallback אופליין ל-Android Document Picker.
- RTL/LTR, ממשק נעים, ואייקון Vector מקורי ללא תמונות.
- קיצור: + ואז - בתוך שנייה מחזיר את המיקוד לשדה הקלט לפתיחה מהירה.

### Build
GitHub Actions ב-`.github/workflows/android.yml` מקמפל Release וחותם אותו בתעודה שנוצרת בזמן הבנייה, ואז מפרסם את ה-APK כ-Artifact.

### App discovery and aliases
The resolver is device-first: it discovers installed apps on the current Android device and matches spoken names against real app labels and package names. The bundled app catalog contains exactly 4,000 unique app names and is used as auxiliary vocabulary, not as proof that an app is installed.

The main screen includes a Settings button with an installed-app manager. You can search every installed app, set one or more personal nicknames, delete them, refresh the inventory, and run an Android-role diagnostic. Personal nicknames take priority over all other matching.

The 4,000-name catalog combines public Google Play category snapshots from privacy-tech-lab/gpc-android with a cleaned public Google Play snapshot. The runtime still uses PackageManager as the authoritative source for what can actually be opened on the device.

ציון Clean Build אחרון: 2026-10-07 — build חדש ומבודד.

Clean fast-loader revision: 2026-10-07.

Clean fast-loader build trigger: 2026-10-07-fix.
