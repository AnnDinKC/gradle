/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.state;

import org.gradle.cache.PersistentIndexedCache;
import org.gradle.caching.internal.BuildCacheHasher;
import org.gradle.caching.internal.DefaultBuildCacheHasher;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hashing;

public class DefaultResourceSnapshotterCacheService implements ResourceSnapshotterCacheService {
    private static final HashCode NO_HASH = Hashing.md5().hashString(CachingResourceHasher.class.getName() + " : no hash");
    private final PersistentIndexedCache<HashCode, HashCode> persistentCache;

    public DefaultResourceSnapshotterCacheService(PersistentIndexedCache<HashCode, HashCode> persistentCache) {
        this.persistentCache = persistentCache;
    }

    @Override
    public HashCode hashFile(String path, Iterable<String> relativePath, FileContentSnapshot content, RegularFileHasher hasher, HashCode configurationHash) {
        HashCode resourceHashCacheKey = resourceHashCacheKey(content, configurationHash);

        HashCode resourceHash = persistentCache.get(resourceHashCacheKey);
        if (resourceHash != null) {
            if (resourceHash.equals(NO_HASH)) {
                return null;
            }
            return resourceHash;
        }

        resourceHash = hasher.hash(path, relativePath, content);

        if (resourceHash != null) {
            persistentCache.put(resourceHashCacheKey, resourceHash);
        } else {
            persistentCache.put(resourceHashCacheKey, NO_HASH);
        }
        return resourceHash;
    }

    private static HashCode resourceHashCacheKey(FileContentSnapshot content, HashCode configurationHash) {
        BuildCacheHasher hasher = new DefaultBuildCacheHasher();
        hasher.putHash(configurationHash);
        hasher.putHash(content.getContentMd5());
        return hasher.hash();
    }
}
