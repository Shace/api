package controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.AccessToken;
import models.Bucket;
import models.Event;
import models.Media;
import play.Logger;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.Controller;

import java.util.Collections;
import java.util.List;

@CORS
public class Buckets extends Controller {
 
    /**
     * Convert a bucket to a JSON object.
     * 
     * @param bucket A bucket object to convert
     * @return The JSON object containing the bucket information
     */
    public static ObjectNode getBucketObjectNode(AccessToken access, Event ownerEvent, Bucket bucket) {
        ObjectNode result = Json.newObject();

        result.put("id", bucket.id);
        result.put("name", bucket.name);
        result.put("level", bucket.level);
        result.put("first", (bucket.first != null) ? (bucket.first.getTime()) : (null));
        result.put("last", (bucket.last != null) ? (bucket.last.getTime()) : (null));
        result.put("size", bucket.size);

        ArrayNode medias = result.putArray("medias");
        if (bucket.medias != null) {
            for (Media media : bucket.medias) {
                medias.add(Medias.mediaToJson(access, ownerEvent, media, false));
            }
        }

        bucket.refresh();
        ArrayNode children = result.putArray("children");
        if (bucket.children != null) {
            if (bucket.children.size() == 0)
                bucket.refresh();
            for (Bucket child : bucket.children) {
                children.add(Buckets.getBucketObjectNode(access, ownerEvent, child));
            }
        }

        return result;
    }
    
    private static final int[] MAXIMUM_DELAYS = {30 * 60, 365*24*60*60, -1};

    @Transactional
    public static void addNewMediaToEvent(Event event, Media media) {
        long startTime = System.nanoTime();

        List<Bucket> buckets = Ebean.find(Bucket.class).fetch("parent").where().eq("event", event).where().eq("level", 0).findList();
        
        /*
         * Get newly added bucket in added variable
         */
        Bucket added = null;
        for (Bucket bucket : buckets) {
            if (bucket.size.equals(0) || 
               (bucket.first.getTime() / 1000 - MAXIMUM_DELAYS[0] <= media.original.getTime() / 1000 &&
               bucket.last.getTime() / 1000 + MAXIMUM_DELAYS[0] >= media.original.getTime() / 1000)) {
                added = bucket;
                break;
            }
        }
        if (added == null) {
            added = new Bucket(0, event);
            added.save();
        }
                        
        mergeLevel(event, 0, added.id, media);
        
        long estimatedTime = System.nanoTime() - startTime;
//        Logger.debug("Time elapsed to compute buckets : " + Long.toString(estimatedTime / 1000000) + "ms");
    }
    
    @Transactional
    private static void mergeLevel(Event event, int level, Integer parentId, Media media) {
        List<Bucket> buckets = Ebean.find(Bucket.class).fetch("parent").where().eq("event", event).where().eq("level", level).orderBy("first asc").findList();

        for (Bucket bucket : buckets) {
            if (bucket.id.equals(parentId)) {
                if (bucket.parent != null) {
                    parentId = bucket.parent.id;
                }
                bucket.medias.add(media);
                if (bucket.size.equals(0) || media.original.getTime() < bucket.first.getTime())
                    bucket.first = media.original;
                if (bucket.size.equals(0) || media.original.getTime() > bucket.last.getTime())
                    bucket.last = media.original;
                bucket.size = bucket.size + 1;
                bucket.save();
            }
        }
        
        Collections.sort(buckets);
        
        for (int i = 0; i < buckets.size(); ++i) {
            Bucket current = buckets.get(i);
            if (i + 1 < buckets.size()) {
                Bucket next = buckets.get(i + 1);
                if (MAXIMUM_DELAYS[level] == -1 ||
                        current.last.getTime() / 1000 + MAXIMUM_DELAYS[level] >= next.first.getTime() / 1000) {
                    
                    if (next.parent != null) {
                        next.parent.size -= next.size;
                        for (Media mediaChild : next.medias)
                            next.parent.medias.remove(mediaChild);
                        next.parent.save();                        
                    }
                    
                    if (next.id.equals(parentId)) {
                        parentId = current.id;
                    }
                    
                    String s = "UPDATE se_bucket_media set se_bucket_id = :new where se_bucket_id = :prev";
                    SqlUpdate update = Ebean.createSqlUpdate(s);
                    update.setParameter("prev", next.id);
                    update.setParameter("new", current.id);
                    Ebean.execute(update);
                    
                    s = "UPDATE se_bucket set parent_id = :new where parent_id = :prev";
                    update = Ebean.createSqlUpdate(s);
                    update.setParameter("prev", next.id);
                    update.setParameter("new", current.id);
                    Ebean.execute(update);
                    
                    current.size += next.size;
                    if (next.last.getTime() > current.last.getTime())
                        current.last = next.last;
                    
                    current.save();
                    if (event.root.id.equals(next.id)) {
                        event.root = current;
                        event.save();
                    }
                    next.delete();
                    buckets.remove(next);
                }
            }
        }
        
        if (buckets.size() > 1) {
            for (Bucket bucket : buckets) {
                if (bucket.parent == null) {                    
                    bucket.parent = new Bucket(bucket.level + 1, event);
                    bucket.parent.children.add(bucket);
                    bucket.parent.size = bucket.size;
                    bucket.parent.first = bucket.first;
                    bucket.parent.last = bucket.last;
                    for (Media mediaChild : bucket.medias) {
                        if (mediaChild.equals(media)) {
                            bucket.parent.size -= 1;
                        } else {
                            bucket.parent.medias.add(mediaChild);
                        }       
                    }
                    bucket.parent.save();
                }
                
                if (bucket.id.equals(parentId)) {
                    parentId = bucket.parent.id;
                }
            }
            mergeLevel(event, level + 1, parentId, media);
        } else if (buckets.size() == 1) {
            event.root = buckets.get(0);
            event.save();
        }
    }
}
