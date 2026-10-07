package model;

import java.util.ArrayList;
import java.util.List;

public class Posting {
    private final int docId;
    private int frequency;
    private final List<Integer> positions;

    public Posting(int docId, int position) {
        this.docId = docId;
        this.frequency = 1;
        this.positions = new ArrayList<>();
        this.positions.add(position);
    }

    public void addPosition(int position) {
        this.positions.add(position);
        this.frequency++;
    }

    public int getDocId() {
        return docId;
    }

    public int getFrequency() {
        return frequency;
    }

    public List<Integer> getPositions() {
        return positions;
    }
}
