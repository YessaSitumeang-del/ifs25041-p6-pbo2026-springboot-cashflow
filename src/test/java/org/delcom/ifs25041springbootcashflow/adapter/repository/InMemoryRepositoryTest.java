package org.delcom.ifs25041springbootcashflow.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRepositoryTest {

    /** Entitas mutable sederhana untuk menguji repository dasar. */
    static class Item {
        private final int id;
        private String name;

        Item(int id, String name) {
            this.id = id;
            this.name = name;
        }

        int getId() {
            return id;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }
    }

    /** Turunan konkret yang tidak meng-override update(). */
    static class ItemRepository extends InMemoryRepository<Item, Integer> {
        ItemRepository() {
            super(Item::getId);
        }
    }

    private ItemRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ItemRepository();
    }

    @Test
    void updateKeepsMutatedEntityStoredByReference() {
        Item item = repository.save(new Item(1, "lama"));
        item.setName("baru");

        repository.update(item);

        assertEquals(1, repository.findAll().size());
        assertEquals("baru", repository.findById(1).orElseThrow().getName());
    }

    @Test
    void saveFindAndDelete() {
        repository.save(new Item(1, "a"));
        repository.save(new Item(2, "b"));

        assertEquals(2, repository.findAll().size());
        assertTrue(repository.findById(2).isPresent());
        assertTrue(repository.findById(99).isEmpty());
        assertTrue(repository.deleteById(1));
        assertFalse(repository.deleteById(1));
    }

    @Test
    void findByFiltersWithPredicate() {
        repository.save(new Item(1, "a"));
        repository.save(new Item(2, "b"));
        repository.save(new Item(3, "a"));

        List<Item> result = repository.findBy(i -> "a".equals(i.getName()));

        assertEquals(2, result.size());
    }
}