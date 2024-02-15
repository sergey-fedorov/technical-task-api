package dtos;

import lombok.Data;

import java.util.List;

@Data
public class People {
    public int count;
    public String next;
    public Object previous;
    public List<Character> results;
}
