-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class ssmg.vhbb_android.** { *; }

-dontwarn org.apache.commons.net.**
-keep class org.apache.commons.net.** { *; }

-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**
-keep class jp.wasabeef.picasso.** { *; }

-dontwarn com.android.volley.**

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
