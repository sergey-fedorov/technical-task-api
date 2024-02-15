package dtos;

import lombok.Data;

import java.util.List;

@Data
public class Films {
    public int count;
    public Object next;
    public Object previous;
    public List<Film> results;
}
