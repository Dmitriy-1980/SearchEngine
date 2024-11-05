package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.repositories.PageRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageCRUDServiceImpl implements PageCRUDService{
    private final PageRepository pageRep;


    @Override
    public Optional<Integer> addPage(Page page) {
        return Optional.empty();
    }

    @Override
    public Optional<Page> getById(int pageId) {
        return Optional.empty();
    }

    @Override
    public boolean upateById(Page page) {
        return false;
    }

    @Override
    public boolean delById(int pageId) {
        return false;
    }

    @Override
    public boolean delAllBySiteId(int siteId) {
        return false;
    }
}
