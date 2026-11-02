package nl.johninnl.flitsmeister;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class NativeWarningTextTest {
    public static class Type {
        public String getLocalizedTTS(Context context){return "native camera "+context.getResources().getConfiguration().getLocales().get(0).getLanguage();}
    }
    public static class SpeedCam {public Type getSpeedCamType(){return new Type();}public String getShortDescription(Context c){throw new AssertionError("UI label must not replace native TTS");}}
    public static class Owner {public Context a;Owner(Context context){a=context;}}
    public static class EarlyWarningNotification {public String getFreetext(){return "Vorsicht hinter Ihnen";}public String getDescription(){return "Generic title";}}
    @Test public void cameraFallbackUsesTheNativeTtsFormatterAndItsActualResourceContext(){
        Context app=RuntimeEnvironment.getApplication();Configuration config=new Configuration(app.getResources().getConfiguration());config.setLocale(Locale.GERMAN);
        Context german=app.createConfigurationContext(config);Context source=NativeWarningText.context(app,new Owner(german));
        NativeWarningText.Text result=NativeWarningText.resolve(source,new SpeedCam(),"enter",null);
        assertEquals("native camera de",result.value);assertEquals("de",result.language);assertEquals("native_resource",result.origin);
    }
    @Test public void externalNativeFreeTextKeepsUnknownLanguageRatherThanAppLocale(){
        NativeWarningText.Text result=NativeWarningText.resolve(RuntimeEnvironment.getApplication(),new EarlyWarningNotification(),"notice",null);
        assertEquals("Vorsicht hinter Ihnen",result.value);assertEquals("und",result.language);assertEquals("native_text",result.origin);
    }
    @Test public void suppliedNativeInstructionsArePreservedWithoutReformatting(){
        String text="Keep left — 500 m";NativeWarningText.Text result=NativeWarningText.resolve(RuntimeEnvironment.getApplication(),new SpeedCam(),"enter",text);
        assertEquals(text,result.value);assertEquals("native_text",result.origin);
    }
    public static class TrafficJam {}
    @Test public void auditedExitTemplateUsesItsOwnLanguageWhenTheSourceLanguageHasNoMapping(){
        Context app=RuntimeEnvironment.getApplication();Configuration config=new Configuration(app.getResources().getConfiguration());config.setLocale(Locale.GERMAN);
        NativeWarningText.Text text=NativeWarningText.resolve(app.createConfigurationContext(config),new TrafficJam(),"exit",null);
        assertEquals("End of traffic jam.",text.value);assertEquals("en",text.language);assertEquals("template",text.origin);
    }
}
