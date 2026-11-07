package org.ungoogled.patches.maps.car

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeNotNull
import org.junit.Test

class MapsGuidanceComponentTest {
    @Test fun suppliedOriginalExposesTheConcreteCachedComponentAndCurrentListener() {
        val path = System.getenv("MAPS_CAR_ORIGINAL_APK"); assumeNotNull(path)
        val dex = DexFileFactory.loadDexContainer(File(path!!), Opcodes.forApi(29))
        val classes = dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }.associateBy { it.type }
        validateMapsGuidanceComponent { classes.getValue(it) }
        validateMapsNavigationProvider { classes.getValue(it) }
        // An old interface can still exist while the actual application no longer
        // implements it. Reject that mismatch before emitting another broken APK.
        val app = classes.getValue("Lcom/google/android/apps/gmm/base/app/GoogleMapsApplication;")
        val disconnected = ImmutableClassDef(app.type, app.accessFlags, "Landroid/app/Application;",
            app.interfaces, app.sourceFile, app.annotations, app.fields, app.methods)
        assertThrows(IllegalStateException::class.java) {
            validateMapsGuidanceComponent { if (it == app.type) disconnected else classes.getValue(it) }
        }
        val binder = classes.getValue("Llvp;")
        val missingDispatch = ImmutableClassDef(binder.type, binder.accessFlags, binder.superclass,
            binder.interfaces, binder.sourceFile, binder.annotations, binder.fields, binder.methods.filter { it.name != "y" })
        assertThrows(IllegalStateException::class.java) {
            validateMapsNavigationProvider { if (it == binder.type) missingDispatch else classes.getValue(it) }
        }
    }
}
