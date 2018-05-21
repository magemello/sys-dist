package org.magemello.sys.node.domain;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class Record {

    //    @JsonIgnore
    //    @GeneratedValue(strategy = GenerationType.AUTO)
    private String _ID;

    @Id
    private String key;

    private String value;

    public Record() {
    }

    public Record(String key, String value) {
        this._ID = UUID.randomUUID().toString();
        this.key = key;
        this.value = value;
    }

    public String get_ID() {
        return _ID;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Record{" +
                "_ID=" + _ID +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
