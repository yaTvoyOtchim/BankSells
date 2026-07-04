# правила по умолчанию достаточно, kotlinx-serialization:
-keep,includedescriptorclasses class com.bank.salestracker.**$$serializer { *; }
-keepclassmembers class com.bank.salestracker.** { *** Companion; }
-keepclasseswithmembers class com.bank.salestracker.** { kotlinx.serialization.KSerializer serializer(...); }
