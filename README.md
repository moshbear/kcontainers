# Kotlin containers

Containers that are used in other projects.

The following are provided:
- copy-on-write MutableList
- copy-on-write MutableMap
- copy-on-write MutableSet
- circular iterator for List

## Copy on first write
We use the modify-copy concept from java.util.concurrent.CopyOnWriteArrayList and adapt it as follows:
there is a kotlin.collections.List unlocked view for read operations and write operations are performed on a copy
of the data while holding a lock.

However, instead of implementing the entire MutableList interface, we use a `<R> write(MutableList.()->R)` method
to centralize the writes. So instead of
```kotlin
val al = java.util.concurrent.CopyOnWriteArrayList<Int>()
// ...
al.add(1)
```
we do
```kotlin
val al = CopyOnWriteArrayList<Int>()
// ...
al.write {
    add(1)
}
```

This makes the code slightly more verbose but makes it easier to do transactional operations like:
```kotlin
val al = CopyOnWriteArrayList<Int>()
val toAdd = getList()
// ...
al.write {
    for (v in toAdd) {
        require(v !in al)
        add(v)
    }
}
```
It also makes it clearer to see where copying is occurring.

The same principle applies for Map and Set, with implementations for HashMap, LinkedHashMap and HashSet.
CopyOnWriteHashSet is expected to be faster than `j.u.c.CopyOnWriteArraySet` because hashtable lookup is faster than a
linear scan.

For custom types, the user may use `CopyOnWriteList`, `CopyOnWriteMap`, or `CopyOnWriteSet` and supply the necessary
initial data and copy constructor parameters.
For more exotic cases, `CopyOnWriteContainer` can be used.

`CopyOnWriteCollection` is unimplemented because it would be unused by both `CopyOnWriteList` and `CopyOnWriteSet` as
the casting necessary to make it work negatively impacts readability without a good reason.
Implementing it would be a trivial copy-paste of `CoWTracer` inside [test/kotlin/CopyOnWriteContainerTest].

## Circular iterator
Inspired by Python's `itertools.cycle`, this presents an infinite cyclical view of a `List<E>`'s contents.

Example:
```kotlin
listOf(1, 2, 3).circularIterator()
// >>> 1, 2, 3, 1, 2, 3, ...
```

This is only implemented as an extension method for List because that is the base interface for ordered collection
with indexed access and both are certainly necessary here. An extension for Collection may be made but it will have a
memory cost because it will have to store a List of the iterator contents then return a List.circularIterator for that
(which Python does).

A neat thing that can be done here is to use subList to yield a circular iterator over part of the List:
```kotlin
val l = listOf(1, 2, 3, 4, 5)
val inner = l.subList(2, 4)
val iter = inner.circularIterator()
iter
// >>> 2, 3, 2, 3, ...
```