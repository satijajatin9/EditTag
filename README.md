# EditTag
[![](https://jitpack.io/v/satijajatin9/EditTag.svg)](https://jitpack.io/#satijajatin9/EditTag)

### How to use

* Add the dependency

```groovy
 repositories {
     maven { url "https://jitpack.io" }
 }
 dependencies {
     implementation 'com.github.satijajatin9:EditTag:{latest version}'
 }
```
* Add EditTag View in your layout resource

```xml
<com.beastblocks.edittag.EditTag
   android:id="@+id/edit_tag_view"
   android:layout_width="match_parent"
   android:layout_height="wrap_content"
   app:tag_layout="@layout/view_sample_tag"
   app:delete_mode_bg="#FF4081"
   app:input_layout="@layout/view_sample_input_tag"/>
```
