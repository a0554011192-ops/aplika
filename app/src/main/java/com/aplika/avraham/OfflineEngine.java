package com.aplika.avraham;

import android.content.*;
import java.io.*;
import java.util.*;

final class OfflineEngine {
 static final int APP_COUNT=4000,ACTION_COUNT=4000,RESPONSE_COUNT=5000,SYN_COUNT=6000;

 static class Resp{
  String he,en;String[] triggers;
  Resp(String h,String e,String[] t){he=h;en=e;triggers=t;}
 }
 static class ActionEntry{
  String he,en;int setting;String code;String[] triggers;
  ActionEntry(String h,String e,int s,String c,String[] t){he=h;en=e;setting=s;code=c;triggers=t;}
 }
 static class AppEntry{String he,en;AppEntry(String h,String e){he=h;en=e;}}

 final ArrayList<Resp> responses=new ArrayList<>();
 final ArrayList<ActionEntry> actions=new ArrayList<>();
 final HashMap<String,String> synonyms=new HashMap<>();
 final HashMap<String,String> commonAliases=new HashMap<>();

 final HashMap<String,ArrayList<Resp>> responseIndex=new HashMap<>();
 final HashSet<String> loadedResponseKeys=new HashSet<>();
 final HashMap<String,ArrayList<ActionEntry>> actionIndex=new HashMap<>();

 volatile boolean chatLoaded=false,commandsLoaded=false,appsLoaded=false;

 OfflineEngine(Context c){
  alias("כרום","chrome");alias("גוגל כרום","google chrome");
  alias("וואטסאפ","whatsapp");alias("ווטסאפ","whatsapp");
  alias("יוטיוב","youtube");alias("מפות","maps");
  alias("וויז","waze");alias("ווייז","waze");
  alias("טלגרם","telegram");alias("דיסקורד","discord");
  alias("אינסטגרם","instagram");alias("טיקטוק","tiktok");
  alias("פייסבוק","facebook");alias("ספוטיפיי","spotify");
  alias("נטפליקס","netflix");alias("גימייל","gmail");alias("ג׳ימייל","gmail");
  alias("דרייב","google drive");alias("גוגל דרייב","google drive");
  alias("תמונות","google photos");alias("גוגל תמונות","google photos");
  alias("קבצים","files");alias("סייר קבצים","files");alias("סייר הקבצים","files");
  alias("מנהל קבצים","files");alias("מנהל הקבצים","files");alias("קבצים שלי","files");
  alias("file manager","files");alias("file explorer","files");alias("my files","files");alias("files","files");
  alias("מצלמה","camera");alias("camera","camera");
  alias("גלריה","gallery");alias("הגלריה","gallery");alias("תמונות","gallery");alias("גלריית תמונות","gallery");
  alias("photo gallery","gallery");alias("gallery","gallery");alias("photos","gallery");
  alias("שעון","clock");alias("השעון","clock");alias("clock","clock");alias("alarm clock","clock");alias("alarms","clock");
  alias("מחשבון","calculator");alias("המחשבון","calculator");alias("calculator","calculator");
  alias("נגן","music");alias("נגן מוזיקה","music");alias("נגן מוסיקה","music");alias("נגן המדיה","media player");
  alias("נגן מדיה","media player");alias("music player","music");alias("media player","media player");
  alias("player","media player");alias("מוזיקה","music");alias("music","music");
  alias("יומן","calendar");alias("לוח שנה","calendar");alias("calendar","calendar");
  alias("דפדפן","browser");alias("browser","browser");alias("אינטרנט","browser");alias("web browser","browser");
  alias("חנות","play store");alias("חנות play","play store");alias("חנות גוגל","play store");
  alias("גוגל פליי","play store");alias("גוגל פלי","play store");alias("גוגל play","play store");
  alias("google play","play store");alias("google play store","play store");alias("play store","play store");
  alias("play","play store");alias("store","play store");alias("פליי","play store");alias("פלי","play store");
  alias("דרייב","google drive");alias("גוגל דרייב","google drive");alias("drive","google drive");alias("google drive","google drive");
  alias("google drive","google drive");alias("google docs","google docs");alias("docs","google docs");
  alias("google sheets","google sheets");alias("sheets","google sheets");
  alias("google photos","google photos");alias("photos","google photos");
  alias("מוזיקה","music");alias("שירים","music");alias("דואר","email");
  alias("הגדרות","settings");
 }

 void alias(String a,String c){commonAliases.put(normalize(a),normalize(c));}

 synchronized void loadChat(Context c){
  if(chatLoaded)return;
  load(c,"synonyms.tsv",3);
  load(c,"responses.tsv",2);
  chatLoaded=true;
 }

 synchronized void loadCommands(Context c){
  if(commandsLoaded)return;
  actions.clear();
  actionIndex.clear();
  load(c,"actions.tsv",1);
  commandsLoaded=true;
 }
 // Load the app catalog only as a compact alias table. Launching still uses the
 // real PackageManager list, so a catalog entry can never invent an installed app.
 synchronized boolean isKnownAppAlias(String s){
  String x=normalize(s);
  if(x.isEmpty())return false;
  if(commonAliases.containsKey(x))return true;
  String canon=canonicalWord(x);
  return !canon.equals(x) || synonyms.containsKey(x);
 }

 synchronized void loadApps(Context c){
  try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("apps.tsv"),"UTF-8"))){
   String l;
   while((l=br.readLine())!=null){
    String[] p=l.split("\\t",-1);
    if(p.length>=4){
     String he=normalize(p[1]), en=normalize(p[2]);
     String canon=en.isEmpty()?he:en;
     if(!he.isEmpty())alias(he,canon);
     if(!en.isEmpty())alias(en,canon);
     for(String t:p[3].split("\\|",-1)){
      if(!normalize(t).isEmpty())alias(t,canon);
     }
    }
   }
  }catch(Exception ignored){}
  appsLoaded=true;
 }


 void load(Context c,String fn,int type){
  try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open(fn),"UTF-8"))){
   String l;
   while((l=br.readLine())!=null){
    String[] p=l.split("\\t",-1);
    if(type==1&&p.length>=6){
     ActionEntry a=new ActionEntry(p[1],p[2],Integer.parseInt(p[3]),p[4],p[5].split("\\|",-1));
     actions.add(a);
     indexAction(a.he,a);indexAction(a.en,a);
     for(String t:a.triggers)indexAction(t,a);
    }else if(type==2&&p.length>=4){
     String responseKey=normalize(p[1])+"\\u0000"+normalize(p[2])+"\\u0000"+normalize(p[3]);
     // The catalog intentionally keeps stable numeric IDs, but several blocks
     // are byte-for-byte duplicates apart from that ID. Do not load the same
     // response five times into the matcher.
     if(!loadedResponseKeys.add(responseKey))continue;
     Resp rr=new Resp(p[1],p[2],p[3].split("\\|",-1));
     responses.add(rr);
     for(String t:rr.triggers)indexResponse(t,rr);
    }else if(type==3&&p.length>=3){
     synonyms.put(normalize(p[1]),normalize(p[2]));
    }
   }
  }catch(Exception ignored){}
 }

 void indexResponse(String raw,Resp r){
  for(String tok:tokenizeCanonical(raw)){
   if(tok.length()<2)continue;
   ArrayList<Resp> list=responseIndex.get(tok);
   if(list==null){list=new ArrayList<>();responseIndex.put(tok,list);}
   if(!list.contains(r))list.add(r);
  }
 }

 void indexAction(String raw,ActionEntry a){
  for(String tok:tokenizeCanonical(raw)){
   if(tok.length()<2)continue;
   ArrayList<ActionEntry> list=actionIndex.get(tok);
   if(list==null){list=new ArrayList<>();actionIndex.put(tok,list);}
   if(!list.contains(a))list.add(a);
  }
 }

 static String normalize(String s){
  return s.toLowerCase(Locale.ROOT).replace("׳","'").replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
 }

 String canonicalWord(String w){
  String x=commonAliases.get(w);
  if(x==null)x=synonyms.get(w);
  return x==null?w:x;
 }

 ArrayList<String> phraseAliases=new ArrayList<>();

 String[] tokenizeCanonical(String s){
  String n=normalize(s);
  if(n.isEmpty())return new String[0];
  String[] w=n.split("\\s+");
  ArrayList<String> out=new ArrayList<>();
  for(int i=0;i<w.length;){
   String best=null,bestCanon=null;
   for(int len=Math.min(5,w.length-i);len>=2;len--){
    StringBuilder b=new StringBuilder();
    for(int j=0;j<len;j++){if(j>0)b.append(' ');b.append(w[i+j]);}
    String phrase=b.toString();
    String canon=commonAliases.get(phrase);
    if(canon==null)canon=synonyms.get(phrase);
    if(canon!=null){best=phrase;bestCanon=canon;break;}
   }
   if(best!=null){
    for(String x:normalize(bestCanon).split("\\s+"))out.add(x);
    i+=best.split(" ").length;
   }else{
    out.add(canonicalWord(w[i]));
    i++;
   }
  }
  return out.toArray(new String[0]);
 }

 static int edit(String a,String b){
  if(a.equals(b))return 0;
  if(Math.abs(a.length()-b.length())>2)return 3;
  int[][] d=new int[a.length()+1][b.length()+1];
  for(int i=0;i<=a.length();i++)d[i][0]=i;
  for(int j=0;j<=b.length();j++)d[0][j]=j;
  for(int i=1;i<=a.length();i++){
   for(int j=1;j<=b.length();j++){
    int v=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
    d[i][j]=Math.min(v,3);
   }
  }
  return d[a.length()][b.length()];
 }

 int score(String q,String text){return keywordScore(q,text);}
 
 int keywordScore(String query,String trigger){
  String qn=normalize(query),tn=normalize(trigger);
  if(qn.isEmpty()||tn.isEmpty())return 0;
  if(qn.equals(tn))return 40;
  int score=qn.contains(tn)?28:0;
  String[] q=tokenizeCanonical(qn),t=tokenizeCanonical(tn);
  HashSet<String> uq=new HashSet<>(Arrays.asList(q));
  for(String a:uq){
   for(String b:t){
    if(a.equals(b)){score+=8;break;}
    if(a.length()>=3&&b.length()>=3&&(a.contains(b)||b.contains(a))){score+=5;break;}
    if(a.length()>=3&&b.length()>=3&&edit(a,b)<=1){score+=3;break;}
   }
  }
  return score;
 }

 Resp quickResponse(String q){
  String n=normalize(q);
  if(n.isEmpty())return null;
  if(hasAny(n,"שלום","היי","הי","hello","hi"))
   return new Resp("שלום! אני אברהם העברי. מה תרצה לעשות?","Hello! I am Avraham HaIvri. What would you like to do?",new String[]{"שלום"});
  if(hasAny(n,"מה השם שלך","השם שלך","איך קוראים לך","who are you","what is your name"))
   return new Resp("השם שלי הוא אברהם העברי.","My name is Avraham HaIvri.",new String[]{"השם שלך"});
  if(hasAny(n,"מה אתה יכול לעשות","מה אתה יודע","יכולות","capabilities","what can you do"))
   return new Resp("אני יכול לענות ממאגר מקומי, לזהות מילות מפתח, לפתוח אפליקציות ולבצע פעולות מערכת נתמכות.","I can answer from a local catalog, detect keywords, open apps, and run supported system actions.",new String[]{"יכולות"});
  if(hasAny(n,"תן לי חידה","תתן לי חידה","חידה","riddle","give me a riddle"))
   return new Resp("הנה חידה: מה יש לו מקשים אבל לא דלתות? מקלדת. אם פתרת, נוודא יחד את התשובה.","Here is a riddle: What has keys but no doors? A keyboard. Check the answer after you think.",new String[]{"חידה"});
  if(hasAny(n,"בדיחה","תספר בדיחה","תן לי בדיחה","joke","tell me a joke"))
   return new Resp("בדיחה: למה המחשב הלך לרופא? כי היו לו יותר מדי חלונות פתוחים.","Joke: Why did the computer go to the doctor? It had too many open windows.",new String[]{"בדיחה"});
  if(hasAny(n,"מה נשמע","מה קורה","מה שלומך","how are you"))
   return new Resp("אני מוכן. כתוב לי מה תרצה לעשות.","I am ready. Tell me what you would like to do.",new String[]{"מה נשמע"});
  if(hasAny(n,"תודה","תודה רבה","thanks","thank you"))
   return new Resp("בשמחה!","You are welcome!",new String[]{"תודה"});
  if(hasAny(n,"עזרה","תעזור לי","איך משתמשים","help"))
   return new Resp("כתוב כמו שאתה מדבר. למשל: „תתן לי חידה מצחיקה”, „מה השם שלך?”, „פתח לי כרום” או „תגביה שמע”.","Write naturally. For example: “give me a funny riddle”, “what is your name?”, “open Chrome”, or “turn up the volume.”",new String[]{"עזרה"});
  return null;
 }

 boolean hasAny(String q,String... words){
  String n=normalize(q);
  for(String w:words)if(n.contains(normalize(w)))return true;
  return false;
 }

 Resp bestResponse(String q,boolean en){
  Resp fast=quickResponse(q);if(fast!=null)return fast;
  if(!chatLoaded)return null;
  return bestResponseIndexed(q);
 }

 Resp bestResponseIndexed(String q){
  String[] qt=tokenizeCanonical(q);
  LinkedHashSet<Resp> candidates=new LinkedHashSet<>();
  for(String tok:qt){
   ArrayList<Resp> list=responseIndex.get(tok);
   if(list!=null)candidates.addAll(list);
  }
  if(candidates.isEmpty()){
   for(String tok:qt)for(Map.Entry<String,ArrayList<Resp>> e:responseIndex.entrySet()){
    if(tok.length()>=3&&e.getKey().length()>=3&&edit(tok,e.getKey())<=1){candidates.addAll(e.getValue());}
   }
  }
  Resp best=null;int bestScore=0;
  for(Resp r:candidates){
   int s=0;
   for(String tr:r.triggers)s=Math.max(s,keywordScore(q,tr));
   if(s>bestScore){bestScore=s;best=r;}
  }
  return bestScore>=8?best:null;
 }

 ActionEntry bestAction(String q){
  if(!commandsLoaded)return null;
  String[] qt=tokenizeCanonical(q);
  LinkedHashSet<ActionEntry> candidates=new LinkedHashSet<>();
  for(String tok:qt){ArrayList<ActionEntry> list=actionIndex.get(tok);if(list!=null)candidates.addAll(list);}
  ActionEntry best=null;int bestScore=0;
  for(ActionEntry a:candidates){
   int s=keywordScore(q,a.he)+keywordScore(q,a.en);
   for(String tr:a.triggers)s=Math.max(s,keywordScore(q,tr)+8);
   if(s>bestScore){bestScore=s;best=a;}
  }
  return bestScore>=10?best:null;
 }

 String canonical(String s){return canonicalWord(normalize(s));}

 Intent settingIntent(int index){
  String[] a={"android.settings.WIFI_SETTINGS","android.settings.BLUETOOTH_SETTINGS","android.settings.AIRPLANE_MODE_SETTINGS","android.settings.NETWORK_OPERATOR_SETTINGS","android.settings.DATA_USAGE_SETTINGS","android.settings.TETHER_SETTINGS","android.settings.VPN_SETTINGS","android.settings.PRIVATE_DNS_SETTINGS","android.settings.DISPLAY_SETTINGS","android.settings.SOUND_SETTINGS","android.settings.NOTIFICATION_SETTINGS","android.settings.APPLICATION_SETTINGS","android.settings.STORAGE_SETTINGS","android.settings.SECURITY_SETTINGS","android.settings.LOCATION_SOURCE_SETTINGS","android.settings.ACCESSIBILITY_SETTINGS","android.settings.BATTERY_SAVER_SETTINGS","android.settings.BATTERY_OPTIMIZATION_SETTINGS","android.settings.DATE_SETTINGS","android.settings.TIME_SETTINGS","android.settings.TIMEZONE_SETTINGS","android.settings.LOCALE_SETTINGS","android.settings.INPUT_METHOD_SETTINGS","android.settings.CAST_SETTINGS","android.settings.NFC_SETTINGS","android.settings.WIRELESS_SETTINGS","android.settings.SYNC_SETTINGS","android.settings.NFC_SETTINGS","android.settings.WIRELESS_SETTINGS","android.settings.SYNC_SETTINGS"};
  return new Intent(a[Math.floorMod(index,a.length)]);
 }
}