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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        if (_ID != null ? !_ID.equals(record._ID) : record._ID != null) return false;
        if (key != null ? !key.equals(record.key) : record.key != null) return false;
        return value != null ? value.equals(record.value) : record.value == null;
    }

    @Override
    public int hashCode() {
        int result = _ID != null ? _ID.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
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
