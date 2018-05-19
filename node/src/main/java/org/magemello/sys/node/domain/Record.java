package org.magemello.sys.node.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Record {

    //    @JsonIgnore
    //    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long _ID;

    @Id
    private String key;

    private String value;

    public Record() {
    }

    public Record(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Long get_ID() {
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

        if (_ID != record._ID) return false;
        if (key != null ? !key.equals(record.key) : record.key != null) return false;
        return value != null ? value.equals(record.value) : record.value == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (_ID ^ (_ID >>> 32));
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
