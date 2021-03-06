package org.apereo.cas.support.saml.services.idp.metadata.cache;

import org.apereo.cas.support.saml.services.SamlRegisteredService;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.opensaml.saml.metadata.resolver.MetadataResolver;

/**
 * An adaptation of metadata resolver which handles the resolution of metadata resources
 * inside a cache. It basically is a fancy wrapper around a cache, and constructs the cache
 * semantics before processing the resolution of metadata for a SAML service.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
public class SamlRegisteredServiceDefaultCachingMetadataResolver implements SamlRegisteredServiceCachingMetadataResolver {

    private static final int MAX_CACHE_SIZE = 10_000;

    private final SamlRegisteredServiceMetadataResolverCacheLoader chainingMetadataResolverCacheLoader;
    private final LoadingCache<SamlRegisteredServiceCacheKey, MetadataResolver> cache;

    public SamlRegisteredServiceDefaultCachingMetadataResolver(final long metadataCacheExpirationMinutes,
                                                               final SamlRegisteredServiceMetadataResolverCacheLoader loader) {
        this.chainingMetadataResolverCacheLoader = loader;
        this.cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfter(new SamlRegisteredServiceMetadataExpirationPolicy(metadataCacheExpirationMinutes))
            .build(this.chainingMetadataResolverCacheLoader);
    }

    @Override
    public MetadataResolver resolve(final SamlRegisteredService service) {
        LOGGER.debug("Resolving metadata for [{}] at [{}].", service.getName(), service.getMetadataLocation());
        val k = new SamlRegisteredServiceCacheKey(service);
        LOGGER.trace("Locating cached metadata resolver using key [{}] for service [{}]", k.getId(), service.getName());
        val resolver = this.cache.get(k);
        LOGGER.debug("Loaded and cached SAML metadata [{}] from [{}]", resolver.getId(), service.getMetadataLocation());
        return resolver;
    }
}
