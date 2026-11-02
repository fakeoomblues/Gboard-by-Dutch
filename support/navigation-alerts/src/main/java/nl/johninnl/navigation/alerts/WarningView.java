package nl.johninnl.navigation.alerts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Semantic display state only. A card or snapshot is never permission to play audio. */
public final class WarningView {
    private String session="";
    private final Map<String,WarningEvent> values=new HashMap<>();
    public synchronized void snapshot(WarningConnection.Snapshot snapshot) {
        session=snapshot.providerSession;values.clear();for(WarningEvent event:snapshot.state)values.put(event.warningId,event);
    }
    public synchronized void events(List<WarningEvent> events) {
        for(WarningEvent event:events) {
            if(!event.providerSession.equals(session)) { session=event.providerSession;values.clear(); }
            WarningEvent previous=values.get(event.warningId);
            if(previous!=null && event.revision<previous.revision)continue;
            if(event.operation==WarningEvent.Operation.CLEAR)values.remove(event.warningId);
            else if(previous!=null || values.size()<WarningJournal.MAX_WARNINGS)values.put(event.warningId,event);
        }
    }
    public synchronized WarningEvent current(long now) {
        values.entrySet().removeIf(entry->!entry.getValue().stateValid(now));
        WarningEvent best=null;
        for(WarningEvent event:values.values())if(WarningEvent.hasText(event.message) && (best==null
                || event.priority.ordinal()<best.priority.ordinal() || event.priority==best.priority && event.sequence>best.sequence))best=event;
        return best;
    }
    public synchronized void clear() { session="";values.clear(); }
}
