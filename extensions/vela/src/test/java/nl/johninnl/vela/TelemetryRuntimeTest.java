package nl.johninnl.vela;
import static org.junit.Assert.*;
import android.app.Application;import android.location.Location;import android.os.Looper;import android.os.SystemClock;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.time.Duration;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class TelemetryRuntimeTest {
 Application app;
 @Before public void setup(){app=RuntimeEnvironment.getApplication();app.getSharedPreferences("vela_settings",0).edit().clear().putBoolean("flock_cameras_on",true).commit();TelemetryRuntime.resetForTest();TelemetryRuntime.init(app,()->{});TelemetryRuntime.select("com.waze");org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(40));}
 @After public void reset(){TelemetryRuntime.resetForTest();}
 public static class Renderer { public Object p=new Object();public long H;public double q=0,w=0; }
 public static class Provider { public Object e;public boolean f; }
 Location location(String provider,Float speed,long age){Location l=new Location(provider);l.setLatitude(38.5449);l.setLongitude(-121.754);l.setAccuracy(4);l.setBearing(90);if(speed!=null)l.setSpeed(speed);l.setElapsedRealtimeNanos((SystemClock.elapsedRealtime()-age)*1000000);l.setTime(System.currentTimeMillis()-age);return l;}
 @Test public void missingNativeSpeedIsUnknownButMeasuredStationaryZeroIsValid(){
  Renderer r=new Renderer();r.H=SystemClock.elapsedRealtime();
  TelemetryRuntime.nativeSample(location("gps",null,0));TelemetryRuntime.carFrame(r);
  assertEquals("—",TelemetryRuntime.carCurrentLabel(r,"0"));assertNull(TelemetryRuntime.speedMps((Float)0f));
  TelemetryRuntime.nativeSample(location("gps",0f,0));
  assertEquals("0",TelemetryRuntime.carCurrentLabel(r,"0"));assertEquals(0,TelemetryRuntime.speedMps((Float)0f),0);
 }
 @Test public void staleNativeSampleCannotBeRevivedByFreshRendererArrival(){
  Renderer r=new Renderer();r.H=SystemClock.elapsedRealtime();
  TelemetryRuntime.nativeSample(location("gps",20f,30000));TelemetryRuntime.carFrame(r);
  assertNull(TelemetryRuntime.lastFix());assertEquals(-1,TelemetryRuntime.fixAt());assertEquals("—",TelemetryRuntime.carCurrentLabel(r,"0"));
 }
 @Test public void replayArrivalClockRequiresNativeReplayAuthorization(){
  Provider provider=new Provider();TelemetryRuntime.bindLocationProvider(provider);
  Location replay=location("replay",20f,0);replay.setElapsedRealtimeNanos(0);replay.setTime(1);
  TelemetryRuntime.nativeSample(replay);assertNull(TelemetryRuntime.lastFix());
  provider.f=true;TelemetryRuntime.nativeSample(replay);assertNotNull(TelemetryRuntime.lastFix());assertEquals(SystemClock.elapsedRealtime(),TelemetryRuntime.fixAt());
 }
 @Test public void providerChoiceDoesNotWriteSavedFlockChoice(){
  assertTrue(app.getSharedPreferences("vela_settings",0).getBoolean("flock_cameras_on",false));
  TelemetryRuntime.select("");assertTrue(app.getSharedPreferences("vela_settings",0).getBoolean("flock_cameras_on",false));
 }
 @Test public void nativeFallbackExpiryPublishesUnknownWithoutAnotherBroadcast(){
  TelemetryRuntime.resetForTest();final int[] updates={0};
  TelemetryRuntime.init(app,()->updates[0]++);TelemetryRuntime.select("com.waze");
  org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
  TelemetryRuntime.nativeSample(location("gps",20f,0));
  org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
  assertNotNull(TelemetryRuntime.phoneSpeedValue());int beforeExpiry=updates[0];
  org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
  assertNull(TelemetryRuntime.phoneSpeedValue());assertTrue("phone must receive expiry invalidation",updates[0]>beforeExpiry);
 }
 @Test public void olderOrPoorSampleCannotDiscardANewerAcceptedSpeed(){
  TelemetryRuntime.nativeSample(location("gps",20f,0));long accepted=TelemetryRuntime.fixAt();
  TelemetryRuntime.nativeSample(location("gps",null,1000));
  Location poor=location("gps",null,0);poor.setAccuracy(100);TelemetryRuntime.nativeSample(poor);
  assertEquals(accepted,TelemetryRuntime.fixAt());assertEquals(20,TelemetryRuntime.phoneSpeedValue(),0);
 }
}
