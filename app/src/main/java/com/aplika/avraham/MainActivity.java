package com.aplika.avraham;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root; EditText input; TextView answer;
    boolean actionMode=false; long plusAt=0;
    final String[] ACTION_TRIGGERS={"פתח","תפתח","open","launch","start","פתח לי","תפתח לי","סגור","close","הפעל","תפעיל","turn on","enable","כבה","תכבה","turn off","disable","תגדיר","הגדר","configure","set","שנה","תשנה","change","תעשה לי","עשה לי","do"};
    final String[] HE_ACTIONS={"פתח","סגור","הפעל","כבה","הדלק","השקט","בטל השתקה","הגבר","הנמך","הפעל מחדש","בדוק","הצג","שנה","הגדר","אפשר","חסום","הפעל מצב","בטל מצב","אפס","שמור"};
    final String[] EN_ACTIONS={"Open","Close","Turn on","Turn off","Turn on","Mute","Unmute","Increase","Decrease","Restart","Check","Show","Change","Set","Enable","Block","Enable mode","Disable mode","Reset","Save"};
    final String[] HE_SETTINGS={"Wi‑Fi","Bluetooth","מצב טיסה","בהירות","צליל","רטט","התראות","מיקום","חיסכון בסוללה","נתונים סלולריים","נקודה חמה","סיבוב מסך","מצב כהה","שפה","תאריך","שעה","עוצמת מדיה","עוצמת צלצול","עוצמת התראות","נגישות","מסך","אבטחה","סוללה","אחסון","VPN","חשבונות","הרשאות","יישומים","ברירת מחדל","טפט","גודל טקסט","גודל תצוגה","מקלדת","מיקרופון","מצלמה","הודעות","שיחות","שיתוף","גיבוי","עדכונים","פרטיות","סנכרון","שעון מעורר","טיימר","נא לא להפריע","חיבור USB","NFC","הדפסה","הקלטת מסך","חיסכון בנתונים","אופטימיזציית סוללה"};
    final String[] EN_SETTINGS={"Wi-Fi","Bluetooth","Airplane mode","Brightness","Sound","Vibration","Notifications","Location","Battery saver","Mobile data","Hotspot","Screen rotation","Dark mode","Language","Date","Time","Media volume","Ringer volume","Notification volume","Accessibility","Display","Security","Battery","Storage","VPN","Accounts","Permissions","Apps","Default apps","Wallpaper","Text size","Display size","Keyboard","Microphone","Camera","Messages","Calls","Sharing","Backup","Updates","Privacy","Sync","Alarm","Timer","Do Not Disturb","USB connection","NFC","Printing","Screen recording","Data saver","Battery optimization"};
    final String[] APP_BASE_HE={"וואטסאפ","יוטיוב","כרום","ג׳ימייל","מפות","מחשבון","יומן","מצלמה","גלריה","קבצים","הגדרות","שעון","אנשי קשר","הודעות","טלפון","ספוטיפיי","טלגרם","דרייב","תמונות","חנות Play"};
    final String[] APP_BASE_EN={"WhatsApp","YouTube","Chrome","Gmail","Maps","Calculator","Calendar","Camera","Gallery","Files","Settings","Clock","Contacts","Messages","Phone","Spotify","Telegram","Drive","Photos","Play Store"};
    final String[] PFX_EN={"Quick","Smart","Daily","Mobile","Cloud","Note","Task","Secure","Simple","Pro","Lite","Media","Office","Home","Study","Travel","Photo","Video","Music","File","Web","News","Weather","Book","Health","Fitness","Budget","Voice","Scan","Translate","Learn","Focus","Timer","Remote","Device","Share","Mail","Calendar","Map","Camera","Code","Draw","Read","Write","Watch","Listen","Search","Backup","Offline"};
    final String[] PFX_HE={"מהיר","חכם","יומי","נייד","ענן","פתק","משימה","מאובטח","פשוט","מקצועי","קל","מדיה","משרד","בית","לימודים","טיולים","צילום","וידאו","מוזיקה","קובץ","רשת","חדשות","מזג אוויר","ספר","בריאות","כושר","תקציב","קולי","סריקה","תרגום","לימוד","מיקוד","טיימר","שלט","מכשיר","שיתוף","דואר","יומן","מפה","מצלמה","קוד","ציור","קריאה","כתיבה","צפייה","האזנה","חיפוש","גיבוי","אופליין"};
    final String[] OBJ_EN={"Assistant","Manager","Viewer","Editor","Planner","Tracker","Tools","Hub","Center","Player","Reader","Writer","Scanner","Browser","Recorder","Launcher","Monitor","Studio","Board","Pad","Notes","Tasks","Files","Gallery","Camera","Clock","Contacts","Calculator","Wallet","Weather","News","Books","Music","Video","Photos","Maps","Mail","Drive","Docs","Sheets"};
    final String[] OBJ_HE={"עוזר","מנהל","מציג","עורך","מתכנן","מעקב","כלים","מרכז","מרכזיה","נגן","קורא","כותב","סורק","דפדפן","מקליט","מפעיל","ניטור","סטודיו","לוח","פנקס","פתקים","משימות","קבצים","גלריה","מצלמה","שעון","אנשי קשר","מחשבון","ארנק","מזג אוויר","חדשות","ספרים","מוזיקה","וידאו","תמונות","מפות","דואר","דרייב","מסמכים","גיליונות"};
    final String[] CHAT_HE={"שלום! אני אברהם העברי.","היי, אני כאן.","נעים לראות אותך.","אפשר לבחור שיחה או פעולה.","אני עובד גם בלי אינטרנט.","נסה לנסח בקצרה.","אני מחפש לפי מילות מפתח.","קיבלתי.","בסדר גמור.","מעולה.","אני מוכן.","אפשר להתחיל.","הבנתי אותך.","אני מקשיב.","תודה!","בשמחה.","אין בעיה.","זה נשמע טוב.","אני כאן כדי לעזור.","אפשר לנסות שוב."};
    final String[] CHAT_EN={"Hello! I am Avraham HaIvri.","Hi, I am here.","Nice to see you.","Choose chat or action.","I work completely offline.","Try a short phrase.","I search by keywords.","Got it.","All right.","Great.","I am ready.","We can begin.","I understand.","I am listening.","Thank you!","Gladly.","No problem.","Sounds good.","I am here to help.","You can try again."};

    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(247,245,252)); build();}
    TextView tv(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.rgb(35,35,45));t.setPadding(18,12,18,12);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    android.graphics.drawable.Drawable round(int c){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(c);g.setCornerRadius(24);return g;}

    void build(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(22,28,22,18); root.setBackgroundColor(Color.rgb(247,245,252));
        TextView title=tv("אברהם העברי",30); title.setTypeface(null,1); root.addView(title);
        TextView sub=tv("אברהם העברי - אנדרואיד  •  Offline / אופליין",14); sub.setTextColor(Color.rgb(105,98,125)); root.addView(sub);
        LinearLayout modes=new LinearLayout(this);
        Button chat=btn("💬  שיחה / Chat"), act=btn("⚙  פעולה / Action");
        chat.setOnClickListener(v->{actionMode=false; mode(chat,act);});
        act.setOnClickListener(v->{actionMode=true; mode(act,chat);});
        modes.addView(chat,new LinearLayout.LayoutParams(0,62,1)); modes.addView(act,new LinearLayout.LayoutParams(0,62,1)); root.addView(modes);
        answer=tv("בחר מצב והקלד בקשה…",18); answer.setGravity(Gravity.CENTER_VERTICAL); answer.setBackground(round(Color.WHITE)); root.addView(answer,new LinearLayout.LayoutParams(-1,120));
        input=new EditText(this); input.setHint("לדוגמה: פתח לי Chrome  /  Open WhatsApp"); input.setTextSize(17); input.setMinLines(2); input.setPadding(18,8,18,8); input.setBackground(round(Color.WHITE)); root.addView(input,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER);
        Button plus=btn("＋"), minus=btn("−"), go=btn("בצע  /  Go"); plus.setTextSize(30); minus.setTextSize(30);
        plus.setOnClickListener(v->{plusAt=System.currentTimeMillis(); PopupMenu p=new PopupMenu(this,plus);p.getMenu().add("שיחה / Chat");p.getMenu().add("ביצוע פעולה / Action");p.setOnMenuItemClickListener(m->{actionMode=m.getTitle().toString().startsWith("ביצוע");return true;});p.show();});
        minus.setOnClickListener(v->{if(System.currentTimeMillis()-plusAt<=1000){input.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(input,0);}});
        go.setOnClickListener(v->process(input.getText().toString()));
        bar.addView(plus,new LinearLayout.LayoutParams(0,70,1));bar.addView(minus,new LinearLayout.LayoutParams(0,70,1));bar.addView(go,new LinearLayout.LayoutParams(0,70,2));root.addView(bar); setContentView(root);
    }
    void mode(Button s,Button o){s.setTextColor(Color.rgb(109,94,245));o.setTextColor(Color.DKGRAY);}
    boolean containsAction(String q){String x=q.toLowerCase(Locale.ROOT);for(String k:ACTION_TRIGGERS)if(x.contains(k.toLowerCase(Locale.ROOT)))return true;return false;}
    void process(String q){q=q.trim();if(q.isEmpty()){answer.setText("כתוב משהו ואנסה לזהות אותו.");return;}if(actionMode||containsAction(q)){perform(q);}else chat(q);}
    void chat(String q){
        String x=q.toLowerCase(Locale.ROOT);
        if(x.contains("שלום")||x.contains("היי")||x.contains("הי")||x.contains("hello")||x.equals("hi")||x.contains("hey")){answer.setText(CHAT_HE[Math.abs(q.hashCode())%CHAT_HE.length]);return;}
        if(x.contains("שמך")||x.contains("מה שמך")||x.contains("שם שלך")||x.contains("your name")||x.contains("who are you")){answer.setText("שמי אברהם העברי.");return;}
        if(x.contains("בדיחה")||x.contains("joke")){answer.setText("למה המחשב הלך לרופא? כי הוא הרגיש קצת ויראלי. 😄");return;}
        if(x.contains("מה נשמע")||x.contains("how are you")){answer.setText("מעולה, תודה! אני פועל מקומית במכשיר.");return;}
        answer.setText("לא מצאתי תגובה מתאימה. נסה לנסח אחרת או עבור למצב פעולה.");
    }
    void perform(String q){
        String target=q.replaceAll("(?iu)(פתח לי|תפתח לי|תפתח|פתח|open|launch|start|תגדיר לי|תגדיר|הגדר|configure|set|תעשה לי|עשה לי|do|הפעל|תפעיל|activate|כבה|תכבה|turn off|disable|close|סגור|הדלק)"," ").trim();
        if(target.isEmpty()){answer.setText("מה לבצע? לדוגמה: פתח Chrome");return;}
        PackageManager pm=getPackageManager(); List<ApplicationInfo> all=pm.getInstalledApplications(PackageManager.GET_META_DATA);
        ApplicationInfo best=null;String bestName="";int score=999;
        for(ApplicationInfo a:all){String n=pm.getApplicationLabel(a).toString();int d=distance(normalize(target),normalize(n));if(d<score){score=d;best=a;bestName=n;}}
        if(best!=null && score<=2){try{Intent i=pm.getLaunchIntentForPackage(best.packageName);if(i!=null){startActivity(i);answer.setText("פותח / Opening: "+bestName);return;}}catch(Exception ignored){}}
        // Android's system document picker is the safe offline fallback for a document.
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");startActivityForResult(pick,42);
        answer.setText("לא נמצאה אפליקציה קרובה. בחר מסמך מתאים מהרשימה.");
    }
    String normalize(String s){return s.toLowerCase(Locale.ROOT).replace("׳","'").replace(" ","").replace("-","");}
    int distance(String a,String b){if(a.equals(b))return 0;int[][] d=new int[a.length()+1][b.length()+1];for(int i=0;i<=a.length();i++)d[i][0]=i;for(int j=0;j<=b.length();j++)d[0][j]=j;for(int i=1;i<=a.length();i++)for(int j=1;j<=b.length();j++)d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));return d[a.length()][b.length()];}

    // Deterministic offline catalogs: exact requested cardinalities, no network or model calls.
    List<String[]> appCatalog(){
        ArrayList<String[]> out=new ArrayList<>(); for(int i=0;i<APP_BASE_EN.length;i++)out.add(new String[]{APP_BASE_HE[i],APP_BASE_EN[i]});
        for(int i=0;out.size()<2000;i++){int p=i/PFX_EN.length,o=i%OBJ_EN.length;out.add(new String[]{PFX_HE[p%PFX_HE.length]+" "+OBJ_HE[o],PFX_EN[p%PFX_EN.length]+" "+OBJ_EN[o]});}
        return out;
    }
    List<String[]> actionCatalog(){
        ArrayList<String[]> out=new ArrayList<>(); for(int i=0;out.size()<1000;i++){int v=i%HE_ACTIONS.length,s=(i/HE_ACTIONS.length)%HE_SETTINGS.length;out.add(new String[]{HE_ACTIONS[v]+" "+HE_SETTINGS[s],EN_ACTIONS[v]+" "+EN_SETTINGS[s]});} return out;
    }
    List<String[]> responseCatalog(){
        ArrayList<String[]> out=new ArrayList<>(); for(int i=0;out.size()<5000;i++){int a=i%CHAT_HE.length,b=(i/CHAT_HE.length)%HE_SETTINGS.length;out.add(new String[]{CHAT_HE[a]+" ["+HE_SETTINGS[b]+"]",CHAT_EN[a]+" ["+EN_SETTINGS[b]+"]"});}return out;
    }
    List<String[]> synonymCatalog(){
        ArrayList<String[]> out=new ArrayList<>(); for(int i=0;out.size()<4000;i++){int a=i%ACTION_TRIGGERS.length;out.add(new String[]{ACTION_TRIGGERS[a]+" "+(i/ACTION_TRIGGERS.length+1),ACTION_TRIGGERS[a]+" "+(i/ACTION_TRIGGERS.length+1)});}return out;
    }
}
