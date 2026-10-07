package com.aplika.avraham;

import android.content.*;
import java.io.*;
import java.util.*;

final class OfflineEngine {
 static final int APP_COUNT=3000,ACTION_COUNT=4000,RESPONSE_COUNT=5000,SYN_COUNT=6000;
 static class AppEntry{String he,en;AppEntry(String h,String e){he=h;en=e;}}
 static class ActionEntry{String he,en;int setting;String code;String[] triggers;ActionEntry(String h,String e,int s,String c,String[] t){he=h;en=e;setting=s;code=c;triggers=t;}}
 static class Resp{String he,en;String[] triggers;Resp(String h,String e,String[] t){he=h;en=e;triggers=t;}}
 final ArrayList<AppEntry> apps=new ArrayList<>();
 final ArrayList<ActionEntry> actions=new ArrayList<>();
 final ArrayList<Resp> responses=new ArrayList<>();
 final HashMap<String,String> synonyms=new HashMap<>();
 final HashMap<String,Resp> responseIndex=new HashMap<>();
 final HashMap<String,ActionEntry> actionIndex=new HashMap<>();
 volatile boolean loaded=false;

 OfflineEngine(Context c){}

 void loadAll(Context c){
  load(c,"synonyms.tsv",3);
  load(c,"responses.tsv",2);
  load(c,"actions.tsv",1);
  load(c,"apps.tsv",0);
  loaded=true;
 }

 void load(Context c,String fn,int type){
  try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open(fn),"UTF-8"))){
   String l;
   while((l=br.readLine())!=null){
    String[] p=l.split("\\t",-1);
    if(type==0&&p.length>=3)apps.add(new AppEntry(p[1],p[2]));
    else if(type==1&&p.length>=6){
     ActionEntry a=new ActionEntry(p[1],p[2],Integer.parseInt(p[3]),p[4],p[5].split("\\|",-1));
     actions.add(a);
     for(String tr:a.triggers)index(actionIndex,tr,a);
     index(actionIndex,a.he,a);index(actionIndex,a.en,a);
    } else if(type==2&&p.length>=4){
     Resp r=new Resp(p[1],p[2],p[3].split("\\|",-1));
     responses.add(r);
     for(String tr:r.triggers)index(responseIndex,tr,r);
    } else if(type==3&&p.length>=3){
     synonyms.put(normalize(p[1]),normalize(p[2]));
    }
   }
  }catch(Exception ignored){}
 }

 <T> void index(HashMap<String,T> map,String raw,T value){
  String k=normalize(raw);
  if(!k.isEmpty())map.put(k,value);
 }

 static String normalize(String s){
  return s.toLowerCase(Locale.ROOT).replace("׳","'").replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
 }

 String canonical(String s){
  String n=normalize(s),x=synonyms.get(n);
  return x==null?n:x;
 }

 String[] tokens(String s){
  String n=normalize(s);
  if(n.isEmpty())return new String[0];
  String[] a=n.split("\\s+");
  for(int i=0;i<a.length;i++){
   String x=synonyms.get(a[i]);
   if(x!=null)a[i]=x;
  }
  return a;
 }

 int score(String q,String text){
  String nq=normalize(q),nt=normalize(text);
  if(nq.isEmpty()||nt.isEmpty())return 0;
  String cq=canonical(nq),ct=canonical(nt);
  if(cq.equals(ct))return 20;
  if(nq.equals(nt))return 20;
  int s=0;
  String[] qs=tokens(q),ts=tokens(text);
  for(String a:qs)for(String b:ts)if(a.equals(b)){s+=6;break;}
  return s;
 }

 Resp bestResponse(String q,boolean en){
  String n=normalize(q);
  if(n.isEmpty())return null;
  String[] builtins={
   "שלום","היי","הי","מה נשמע","מי אתה","מה אתה יכול לעשות","עזרה",
   "hello","hi","how are you","who are you","help"
  };
  String[] heb={
   "שלום! אני אברהם העברי. אני עובד אופליין ויכול לחפש, לפתוח אפליקציות ולעזור בבקשות מערכת מוכרות.",
   "היי. אני כאן ועובד מקומית על המכשיר.",
   "היי. כתוב לי מה לבצע ואני אנסה לזהות את הבקשה.",
   "אני בסדר. מה תרצה שאעשה?",
   "אני אברהם העברי, מנוע עזר אופליין לאנדרואיד.",
   "אני יכול לזהות בקשות, לחפש אפליקציות, לבצע פעולות מערכת נתמכות ולהגיב ממאגר מקומי.",
   "אפשר לכתוב לי בקשה רגילה. המנוע מנסה להבין מילות מפתח, שמות וכינויים בלי מודל חיצוני.",
   "שלום! אני עובד אופליין.",
   "היי. אני זמין.",
   "אני בסדר. תודה.",
   "אני אברהם העברי.",
   "אני יכול לעזור עם אפליקציות, הגדרות, שמע, ניווט ופעולות מוכרות.",
   "עזרה זמינה מתוך הצ׳אט.",
   "אפשר לנסות: פתח לי כרום, תגביה שמע, או תעביר לשיר הבא."
  };
  for(int i=0;i<builtins.length;i++)if(n.equals(normalize(builtins[i])))return new Resp(heb[i%heb.length],"Hi. I am Avraham HaIvri, an offline Android assistant.",new String[]{builtins[i]});
  Resp r=responseIndex.get(n);
  if(r!=null)return r;
  for(String t:tokens(q)){r=responseIndex.get(t);if(r!=null)return r;}
  return null;
 }

 ActionEntry bestAction(String q){
  String n=normalize(q);ActionEntry a=actionIndex.get(n);if(a!=null)return a;
  for(String t:tokens(q)){a=actionIndex.get(t);if(a!=null)return a;}
  return null;
 }

 AppEntry bestApp(String q){
  String n=normalize(q),cn=canonical(n);
  for(AppEntry a:apps){
   if(cn.equals(canonical(a.en))||cn.equals(canonical(a.he)))return a;
  }
  return null;
 }

 Intent settingIntent(int index){
  String[] a={"android.settings.WIFI_SETTINGS","android.settings.BLUETOOTH_SETTINGS","android.settings.AIRPLANE_MODE_SETTINGS","android.settings.NETWORK_OPERATOR_SETTINGS","android.settings.DATA_USAGE_SETTINGS","android.settings.TETHER_SETTINGS","android.settings.VPN_SETTINGS","android.settings.PRIVATE_DNS_SETTINGS","android.settings.DISPLAY_SETTINGS","android.settings.SOUND_SETTINGS","android.settings.NOTIFICATION_SETTINGS","android.settings.APPLICATION_SETTINGS","android.settings.STORAGE_SETTINGS","android.settings.SECURITY_SETTINGS","android.settings.LOCATION_SOURCE_SETTINGS","android.settings.ACCESSIBILITY_SETTINGS","android.settings.BATTERY_SAVER_SETTINGS","android.settings.BATTERY_OPTIMIZATION_SETTINGS","android.settings.DATE_SETTINGS","android.settings.TIME_SETTINGS","android.settings.TIMEZONE_SETTINGS","android.settings.LOCALE_SETTINGS","android.settings.INPUT_METHOD_SETTINGS","android.settings.CAST_SETTINGS","android.settings.NFC_SETTINGS","android.settings.WIRELESS_SETTINGS","android.settings.SYNC_SETTINGS"};
  return new Intent(a[Math.floorMod(index,a.length)]);
 }
}