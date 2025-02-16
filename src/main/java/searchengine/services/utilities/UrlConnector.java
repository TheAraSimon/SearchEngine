package searchengine.services.utilities;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.ConnectionProfile;

import java.io.IOException;

public class UrlConnector {
    private final ConnectionProfile connectionProfile;
    private final String url;
    @Getter
    private Integer statusCode;
    @Getter
    private Document document;

    public UrlConnector(String url, ConnectionProfile connectionProfile) throws IOException {
        this.url = url;
        this.connectionProfile = connectionProfile;
        connectToUrl();
    }

    private void connectToUrl() throws IOException {
        Connection.Response response = Jsoup.connect(url)
                .userAgent(connectionProfile.getUserAgent())
                .referrer(connectionProfile.getReferrer())
                .timeout(5000)
                .ignoreHttpErrors(true)
                .execute();

        this.statusCode = response.statusCode();
        this.document = response.parse();
    }
}
