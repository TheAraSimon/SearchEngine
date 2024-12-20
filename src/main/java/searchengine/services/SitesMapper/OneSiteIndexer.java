//package searchengine.services.SitesMapper;
//
//import lombok.AllArgsConstructor;
//import lombok.RequiredArgsConstructor;
//import org.hibernate.exception.ConstraintViolationException;
//import org.jsoup.Connection;
//import org.jsoup.Jsoup;
//import org.jsoup.UnsupportedMimeTypeException;
//import org.jsoup.nodes.Document;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//import searchengine.config.ConnectionProfile;
//import searchengine.config.Site;
//import searchengine.model.Page;
//import searchengine.model.Status;
//import searchengine.repositories.PageRepository;
//import searchengine.repositories.SiteRepository;
//
//import java.net.SocketTimeoutException;
//import java.time.Instant;
//import java.util.concurrent.ForkJoinPool;
//
//@AllArgsConstructor
//@Component
//public class OneSiteIndexer {
//    private final PageRepository pageRepository;
//    private final SiteRepository siteRepository;
//    private final ConnectionProfile connectionProfile;
//    private final Site site;
//
//    @Async
//    @Transactional
//    public void indexSite() {
//        ForkJoinPool pool = new ForkJoinPool();
//        WebPageNode rootNode = new WebPageNode(site.getUrl());
//        pool.invoke(new SiteMapper(site.getUrl(), rootNode, site.getUrl(), connectionProfile, pageRepository, siteRepository));
//        pool.shutdown();
////        if (!pageRepository.existsPageByPath(rootNode.getUrl().substring(site.getUrl().length()))) {
////        savePageInPageRepository(rootNode);
////        searchengine.model.Site updatedSite = siteRepository.findSiteByUrl(site.getUrl());
////        updatedSite.setStatus(Status.INDEXED);
////        updatedSite.setStatusTime(Instant.now());
////        siteRepository.save(updatedSite);
//    }
//
////    @Transactional
////    public void savePageInPageRepository(PageNode pageNode) {
////        searchengine.model.Site siteToUpdate = siteRepository.findSiteByUrl(site.getUrl());
////        try {
////            Page page = new Page();
////            page.setSite(siteToUpdate);
////            page.setPath(pageNode.getUrl().substring(siteToUpdate.getUrl().length()));
////            Connection.Response response = Jsoup.connect(pageNode.getUrl())
////                    .userAgent(connectionProfile.getUserAgent())
////                    .referrer(connectionProfile.getReferrer())
////                    .timeout(5000)
////                    .ignoreHttpErrors(true)
////                    .execute();
////            page.setCode(response.statusCode());
////            Document document = response.parse();
////            page.setContent(document.html());
////            siteToUpdate.setStatusTime(Instant.now());
////            pageRepository.save(page);
////            siteRepository.save(siteToUpdate);
////        } catch (UnsupportedMimeTypeException e) {
////            System.out.println(pageNode.getUrl());
////            System.out.println(e.getMessage());
////            System.out.println("Ссылка ".concat(pageNode.getUrl()).concat(" не является сайтом"));
////            // прописать в логах
////        } catch (DataIntegrityViolationException |
////                 ConstraintViolationException e) {
////            System.out.println(pageNode.getUrl());
////            System.out.println(e.getMessage());
////            System.out.println("DataIntegrityViolationException");
////        } catch (SocketTimeoutException e) {
////            System.out.println(pageNode.getUrl());
////            System.out.println(e.getMessage());
////            System.out.println("java.net.SocketTimeoutException: Read timed out");
////        } catch (Exception e) {
////            System.out.println(pageNode.getUrl());
////            System.out.println(e.getMessage());
////            e.printStackTrace();
////            // прописать в логах
////        }
////    }
//}
