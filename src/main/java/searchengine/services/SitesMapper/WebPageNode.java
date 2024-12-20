package searchengine.services.SitesMapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WebPageNode {
    private final String url;
    private final List<WebPageNode> children;

    public WebPageNode(String url) {
        this.url = url;
        this.children = new ArrayList<>();
    }
    public void addChild(WebPageNode child) {
        children.add(child);
    }
}
