package patches.maps.gearslip;

import static org.junit.Assert.*;
import android.os.Bundle;
import android.os.Parcel;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.serialization.Bundleable;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public final class NativeScreenStackTest {
    private static Bundle page(String id){
        Bundle value=CarProtocol.pageTemplate("Maps",true,false,Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),null,7);
        CarProtocol.field(value,"mId",CarProtocol.primitive(id));return value;
    }
    private static TemplateWrapper decode(Bundle value)throws Exception{
        Parcel wire=Parcel.obtain();try{wire.writeBundle(value);wire.setDataPosition(0);return (TemplateWrapper)Bundleable.CREATOR.createFromParcel(wire).get();}finally{wire.recycle();}
    }
    @Test public void nativeBackReturnsToTheObservedParentRatherThanAFabricatedNavigationScreen()throws Exception{
        NativeScreenStack stack=new NativeScreenStack();Bundle home=page("morphe-maps-home");stack.apply(home);
        Bundle input=CarProtocol.searchTemplate("","Search",new android.os.Binder());stack.apply(input);
        assertEquals(2,decode(input).getTemplateInfosForScreenStack().size());
        assertEquals("morphe-maps-home",decode(input).getTemplateInfosForScreenStack().get(1).getTemplateId());
        Bundle preview=page("morphe-maps-preview");stack.apply(preview);assertEquals(3,decode(preview).getTemplateInfosForScreenStack().size());
        stack.apply(input);assertEquals(2,decode(input).getTemplateInfosForScreenStack().size());
        stack.apply(home);assertEquals(1,decode(home).getTemplateInfosForScreenStack().size());
    }
    @Test public void guidanceStartsANewTaskAndRepeatedTemplatesDoNotGrowTheStack()throws Exception{
        NativeScreenStack stack=new NativeScreenStack();stack.apply(page("morphe-maps-home"));stack.apply(page("morphe-maps-preview"));
        Bundle guidance=CarProtocol.template(null);stack.apply(guidance);assertEquals(1,decode(guidance).getTemplateInfosForScreenStack().size());
        Bundle input=CarProtocol.searchTemplate("","Search",new android.os.Binder());stack.apply(input);stack.apply(input);assertEquals(2,decode(input).getTemplateInfosForScreenStack().size());
        stack.apply(guidance);assertEquals(1,decode(guidance).getTemplateInfosForScreenStack().size());
    }
    @Test public void taskClassificationMatchesTheNormalSdkClientInsteadOfStackDepth()throws Exception{
        NativeScreenStack stack=new NativeScreenStack();
        for(String id:new String[]{"morphe-maps-home","morphe-maps-preview","morphe-maps-details","morphe-maps-preview"}){
            Bundle value=page(id);stack.apply(value);TemplateWrapper actual=decode(value),normal=TemplateWrapper.wrap(actual.getTemplate());
            assertEquals("Task depth is a host classification, not the app screen count",normal.getCurrentTaskStep(),actual.getCurrentTaskStep());
            assertEquals("Leave refresh classification with the host",normal.isRefresh(),actual.isRefresh());
        }
    }
}
