package space.kiibou.byteguard.specification;

import space.kiibou.byteguard.Guard;

public record GuardState(Guard guard, Guard.State state) {
}
