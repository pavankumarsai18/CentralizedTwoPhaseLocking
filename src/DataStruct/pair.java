package DataStruct;

public class pair<X, Y> {
    public static final String TAG = pair.class.getName();
    private X first;
    private Y second;

    public pair(X first, Y second)
    {
        this.first = first;
        this.second = second;
    }

    public X getFirst() {
        return first;
    }

    public Y getSecond() {
        return second;
    }

    public void setSecond(Y second) {
        this.second = second;
    }

    public void setFirst(X first) {
        this.first = first;
    }
}
