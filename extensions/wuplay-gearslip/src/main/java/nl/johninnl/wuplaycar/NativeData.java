package nl.johninnl.wuplaycar;

import android.content.Context;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Uses the installed app's selected profile, caches, filters and add-on transport. */
final class NativeData implements AutoCloseable {
    static final class PinRequired extends Exception { PinRequired(){super("Unlock your selected WuPlay profile");} }
    private final ProfileAccess access=new ProfileAccess();private Thread observer;
    void watchProfiles(Runnable changed) throws Exception {
        final Object[] previous=new Object[3];final boolean[] initial={true};
        observer=calls.watch(NativeCalls.field(settings,"activeProfile"),profile->{
            String id=NativeCalls.text(profile,"id"),key=NativeCalls.text(profile,"profileKey");Object pin=NativeCalls.optional(profile,"hasPinSet");
            boolean differs=!Objects.equals(previous[0],id)||!Objects.equals(previous[1],key)||!Objects.equals(previous[2],pin);
            previous[0]=id;previous[1]=key;previous[2]=pin;
            if(initial[0]){initial[0]=false;return;}
            if(differs){access.clear();changed.run();}
        });
    }
    private static final class ProfileStamp {
        final String id,key;ProfileStamp(String id,String key){this.id=id;this.key=key;}
    }
    private ProfileStamp begin() throws Exception {
        Object profile=calls.first(NativeCalls.field(settings,"activeProfile"));
        if(profile==null)throw new IllegalStateException("Select a profile in WuPlay on your phone");
        String id=(String)NativeCalls.field(profile,"id"),key=(String)NativeCalls.field(profile,"profileKey");
        if(id==null||key==null||id.isEmpty()||key.isEmpty())throw new IllegalStateException("WuPlay profile is not ready");
        if(!access.allowed(id,key,(Boolean)NativeCalls.field(profile,"hasPinSet")))throw new PinRequired();
        return new ProfileStamp(id,key);
    }
    boolean verifyPin(String pin) throws Exception {
        Object profile=calls.first(NativeCalls.field(settings,"activeProfile"));
        if(profile==null)return false;
        String id=NativeCalls.text(profile,"id"),key=NativeCalls.text(profile,"profileKey");
        access.allowed(id,key,Boolean.TRUE.equals(NativeCalls.optional(profile,"hasPinSet")));
        Object token=calls.first(NativeCalls.field(settings,"deviceToken"));
        if(!(token instanceof String)||((String)token).isEmpty())return false;
        Object backend=NativeCalls.call(container,"getBackendHttpClient");
        Object verifier=NativeCalls.make("app.wuplay.androidtv.network.BackendHttpClient$setPin$$inlined$safeApiCall$default$1",backend,null,backend,pin,key,token,3);
        Object io=Class.forName("kotlinx.coroutines.scheduling.DefaultIoScheduler").getField("INSTANCE").get(null);
        Object result=calls.await(Class.forName("kotlinx.coroutines.JobKt"),"withContext",io,verifier);
        if(result==null||!result.getClass().getName().equals("app.wuplay.androidtv.network.ApiResult$Success"))return false;
        Object verification=NativeCalls.field(result,"data");
        if(verification==null||!verification.getClass().getName().equals("app.wuplay.androidtv.network.PinVerifyResponse")||!Boolean.TRUE.equals(NativeCalls.field(verification,"valid")))return false;
        Object current=calls.first(NativeCalls.field(settings,"activeProfile"));
        if(current==null||!id.equals(NativeCalls.text(current,"id"))||!key.equals(NativeCalls.text(current,"profileKey")))return false;
        return access.unlock(id,key);
    }
    static final class Catalog {
        final Object value; final String title,source; final int order;
        Catalog(Object value) throws Exception {this.value=value;title=NativeCalls.text(value,"name");source=NativeCalls.text(value,"source");order=(Integer)NativeCalls.field(value,"homeOrder");}
    }
    static final class Item {
        final String type,id,title,subtitle,parentId; final Object value;
        final Integer relative; final List<?> videos;
        Item(String type,String id,String title,String subtitle,String parentId,Integer relative,Object value,List<?> videos) {
            this.type=type;this.id=id;this.title=title;this.subtitle=subtitle;this.parentId=parentId;this.relative=relative;this.value=value;this.videos=videos;
        }
        static Item preview(Object value) {
            return new Item(NativeCalls.text(value,"type"),NativeCalls.text(value,"id"),NativeCalls.text(value,"name"),NativeCalls.text(value,"description"),null,null,value,Collections.emptyList());
        }
    }
    static final class Choice {
        final Object value;final String title,url,addonUrl,addonName;
        Choice(Object stream,String addonUrl,String addonName) {
            value=stream;url=NativeCalls.text(stream,"url");this.addonUrl=addonUrl;this.addonName=addonName;
            String name=NativeCalls.text(stream,"name"),detail=NativeCalls.text(stream,"title");
            if(detail.isEmpty())detail=NativeCalls.text(stream,"description");
            title=(name.isEmpty()?addonName:name)+(detail.isEmpty()?"":" · "+detail);
        }
        @SuppressWarnings("unchecked") Map<String,String> headers() {
            Object hints=NativeCalls.optional(value,"behaviorHints"),proxy=NativeCalls.optional(hints,"proxyHeaders"),request=NativeCalls.optional(proxy,"request");
            return request instanceof Map ? (Map<String,String>)request : Collections.emptyMap();
        }
    }
    final Object container,settings;private final Object catalogs,addons,metadata;
    private final NativeCalls calls=new NativeCalls();
    NativeData(Context context) throws Exception {
        container=NativeCalls.field(context.getApplicationContext(),"container");
        settings=NativeCalls.field(container,"userSettingsRepository");
        catalogs=NativeCalls.field(container,"catalogRepository");addons=NativeCalls.field(container,"addonRepository");metadata=NativeCalls.field(container,"metaRepository");
    }
    String profileId() throws Exception {return (String)calls.first(NativeCalls.field(settings,"activeProfileId"));}
    String profileName() throws Exception {return NativeCalls.text(calls.first(NativeCalls.field(settings,"activeProfile")),"name");}
    private void requireProfile(ProfileStamp expected) throws Exception {
        ProfileStamp current=begin();
        if(!Objects.equals(expected.id,current.id)||!Objects.equals(expected.key,current.key))throw new CancellationException("Profile changed; reload WuPlay in the car");
    }
    List<Catalog> catalogs() throws Exception {
        ProfileStamp profile=begin();calls.await(catalogs,"awaitSchemaReady");List<Catalog> out=new ArrayList<>();
        for(Object value:(List<?>)calls.first(NativeCalls.field(catalogs,"definitions"))) {
            if(Boolean.TRUE.equals(NativeCalls.field(value,"showHome")))out.add(new Catalog(value));
        }
        out.sort(Comparator.comparingInt(c->c.order));requireProfile(profile);return out;
    }
    CatalogWindow<Item> items(Catalog catalog,int skip) throws Exception {
        ProfileStamp profile=begin();Map<String,String> extra=skip==0?Collections.emptyMap():Collections.singletonMap("skip",Integer.toString(skip));
        boolean remote=catalog.source.equals("addon");
        Object result=remote?calls.await(catalogs,"fetchContent-BWLJW6A",catalog.value,extra,null):calls.await(catalogs,"fetchGridContent-0E7RQCE",catalog.value,null);
        String error=NativeCalls.text(result,"error");if(!error.isEmpty())throw new IllegalStateException(error);
        List<Item> out=new ArrayList<>();for(Object value:(List<?>)NativeCalls.field(result,"items"))out.add(Item.preview(NativeCalls.call(Class.forName("androidx.core.math.MathUtils"),"withNormalizedId",value)));
        int next=-1;
        if(remote){int consumed=skip==0?(Integer)NativeCalls.field(result,"unfilteredCount"):out.size();if(consumed>0)next=skip+consumed;}
        requireProfile(profile);return new CatalogWindow<>(out,next);
    }
    Item details(Item item) throws Exception {
        ProfileStamp profile=begin();Object response=calls.await(metadata,"getMeta-yxL6bBk",item.type,item.parentId==null?item.id:item.parentId,false,null);
        NativeMetadata.Value decoded=NativeMetadata.decode(response);
        Object meta=NativeCalls.call(Class.forName("androidx.core.math.MathUtils"),"withNormalizedId",decoded.meta);
        String id=NativeCalls.text(meta,"id"),description=decoded.description;
        if(description.isEmpty())description=item.subtitle;
        requireProfile(profile);
        return new Item(item.type,item.parentId==null&&!id.isEmpty()?id:item.id,item.title,description,item.parentId,item.relative,meta,decoded.videos);
    }
    List<Item> episodes(Item item) throws Exception {
        Item detail=details(item);List<Item> out=new ArrayList<>();
        for(Object video:detail.videos) {
            Object season=NativeCalls.optional(video,"season"),episode=NativeCalls.optional(video,"episode");
            String label=(season==null?"":"S"+season)+(episode==null?"":" E"+episode)+" · "+NativeCalls.text(video,"title");
            out.add(new Item("series",NativeCalls.text(video,"id"),item.title,label,detail.id,(Integer)NativeCalls.optional(video,"seasonRelativeEpisode"),detail.value,detail.videos));
        }
        return out;
    }
    List<Choice> streams(Item item) throws Exception {
        ProfileStamp profile=begin();List<Choice> out=new ArrayList<>();int failures=0,queried=0;
        for(Object addon:(List<?>)calls.first(NativeCalls.field(addons,"enabledAddons"))) {
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            List<?> resources=(List<?>)NativeCalls.field(addon,"resources");if(!resources.isEmpty()&&!resources.contains("stream"))continue;
            if(!Boolean.TRUE.equals(calls.await(addons,"allowsStreamQuery",addon,item.type,item.id)))continue;
            queried++;
            try {
                String url=NativeCalls.text(addon,"url"),name=NativeCalls.text(addon,"name");
                Object result=calls.await(addons,"fetchStreams-hUnOzRk",url,item.type,item.id,Collections.emptyMap(),item.relative);
                for(Object stream:(List<?>)NativeCalls.field(result,"streams"))out.add(new Choice(stream,url,name));
            } catch(CancellationException e){throw e;} catch(InterruptedException e){throw e;} catch(Exception e){failures++;}
            requireProfile(profile);
        }
        if(out.isEmpty()&&queried>0&&failures==queried)throw new IllegalStateException("The enabled add-ons could not load streams. Retry or check WuPlay on your phone.");
        requireProfile(profile);return out;
    }
    @Override public void close(){access.clear();calls.close();if(observer!=null)observer.interrupt();}
}
