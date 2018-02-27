package de.otto.synapse.state;

import net.openhft.chronicle.hash.ChronicleHashClosedException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.IntStream.range;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ChronicleMapStateRepositoryTest {

    @Test
    public void shouldRetrieveValueAfterPut() throws Exception {
        // given
        ChronicleMapStateRepository<SomePojo> repository = ChronicleMapStateRepository.builder(SomePojo.class).build();
        // when
        repository.put("someKey", new SomePojo("A", 1));
        Optional<SomePojo> result = repository.get("someKey");
        // then
        Assert.assertTrue(result.isPresent());
        assertThat(result.get(), is(new SomePojo("A", 1)));
    }

    @Test
    public void shouldReturnOptionalEmptyForUnknownKey() throws Exception {
        // given
        ChronicleMapStateRepository<SomePojo> repository = ChronicleMapStateRepository.builder(SomePojo.class).build();
        // when
        Optional<SomePojo> result = repository.get("someUnknownKey");
        // then
        Assert.assertTrue(!result.isPresent());
    }

    @Test
    public void shouldReturnOptionalEmptyForRemovedEntry() throws Exception {
        // given
        ChronicleMapStateRepository<SomePojo> repository = ChronicleMapStateRepository.builder(SomePojo.class).build();
        repository.put("someKey", new SomePojo("A", 1));
        // when
        repository.remove("someKey");
        Optional<SomePojo> result = repository.get("someKey");
        // then
        Assert.assertTrue(!result.isPresent());
    }

    @Test
    public void shouldCalculateCorrectSizeWhenRemovingEntries() throws Exception {
        // given
        ChronicleMapStateRepository<SomePojo> repository = ChronicleMapStateRepository.builder(SomePojo.class).build();

        ExecutorService executorService = newFixedThreadPool(20);

        // when
        range(0, 9).forEach((i) ->
                executorService.submit(() ->
                        range(0, 1000).forEach((j) ->
                                {
                                    int randomNum = ThreadLocalRandom.current().nextInt(0, 10000000);
                                    //System.out.println(Integer.toBinaryString(randomNum));
                                    repository.put("someKey", new SomePojo(Integer.toBinaryString(randomNum), randomNum));
                                }
                        )
                )
        );
        range(0, 9).forEach((i) -> executorService.submit(() ->
                range(0, 1000).forEach((j) -> repository.remove("someKey")))
        );

        executorService.shutdown();
        repository.remove("someKey");

        assertThat(repository.bytesUsed.get(), is(0L));
    }

    @Test
    public void shouldReturnCorrectEntrySize() throws Exception {
        // given
        ChronicleMapStateRepository<SomePojo> repository = ChronicleMapStateRepository.builder(SomePojo.class).build();
        repository.put("someKeyA", new SomePojo("A", 1));
        repository.put("someKeyB", new SomePojo("B", 2));
        repository.put("someKeyC", new SomePojo("C", 3));
        // when
        long resultSize = repository.size();
        // then
        assertThat(resultSize, is(3L));
    }

    @Test
    public void shouldIterateOverKeySet() throws Exception {
        // given
        ChronicleMapStateRepository<SomePojo> repository = ChronicleMapStateRepository.builder(SomePojo.class).build();
        repository.put("someKeyA", new SomePojo("A", 1));
        repository.put("someKeyB", new SomePojo("B", 2));
        repository.put("someKeyC", new SomePojo("C", 3));
        // when
        List<String> resultKeys = new ArrayList<>();
        repository.getKeySetIterable().forEach(resultKeys::add);
        // then
        assertThat(resultKeys, containsInAnyOrder("someKeyA", "someKeyB", "someKeyC"));
    }

    @Test
    public void shouldNotPutIntoClosedCache() {
        ChronicleMapStateRepository<SomePojo> cache = ChronicleMapStateRepository.builder(SomePojo.class).build();
        cache.close();

        // when
        SomePojo testPojo = new SomePojo("A", 1);
        try {
            cache.put("someId", testPojo);
        } catch (ChronicleHashClosedException e) {
            // then
            Assert.fail("Tried to put item into a closed cache");
        }
    }

    @Test
    public void shouldNotGetFromClosedCache() {
        ChronicleMapStateRepository<SomePojo> cache = ChronicleMapStateRepository.builder(SomePojo.class).build();
        // when
        SomePojo testPojo = new SomePojo("A", 1);
        cache.put("someId", testPojo);
        cache.close();

        try {
            cache.get("someId");
        } catch (ChronicleHashClosedException e) {
            // then
            Assert.fail("Tried to get item from a closed cache");
        }
    }

    @Test
    public void shouldReturnZeroSizeForClosedCache() {
        ChronicleMapStateRepository<SomePojo> cache = ChronicleMapStateRepository.builder(SomePojo.class).build();
        // when
        SomePojo testPojo = new SomePojo("A", 1);
        cache.put("someId", testPojo);
        cache.close();

        try {
            Assert.assertEquals("Cache should have zero size", 0, cache.size());
        } catch (ChronicleHashClosedException e) {
            // then
            Assert.fail("Tried to get size of a closed cache - should not throw exception");
        }
    }

    @Test
    public void shouldNotGetFullCacheLogFromClosedCache() {
        ChronicleMapStateRepository<SomePojo> cache = ChronicleMapStateRepository.builder(SomePojo.class).build();
        cache.close();

        // when
        try {
            cache.getStats();
        } catch (ChronicleHashClosedException e) {
            // then
            Assert.fail("Tried to get memory log from closed cache");
        }


    }

    public static class SomePojo {

        public String someString;
        public int someInteger;

        // for json serialization
        SomePojo() {
        }

        SomePojo(String someString, int someInteger) {
            this.someString = someString;
            this.someInteger = someInteger;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SomePojo somePojo = (SomePojo) o;
            return someInteger == somePojo.someInteger &&
                    Objects.equals(someString, somePojo.someString);
        }

        @Override
        public int hashCode() {
            return Objects.hash(someString, someInteger);
        }
    }
}
