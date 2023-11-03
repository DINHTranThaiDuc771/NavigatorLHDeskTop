package navigatorlh.Util;

public class NodeQuad {
    Point pos;
    protected String data;

    public NodeQuad(Point _pos, String _data) {
        pos = _pos;
        data = _data;
    }

    public NodeQuad() {
        data = "";
    }
    public String getData()
    {
        return this.data;
    }
}

