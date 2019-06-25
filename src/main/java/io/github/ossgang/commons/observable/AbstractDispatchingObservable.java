// @formatter:off
/*******************************************************************************
 *
 * This file is part of ossgang-commons.
 *
 * Copyright (c) 2008-2019, CERN. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
// @formatter:on

package io.github.ossgang.commons.observable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractDispatchingObservable<T> implements Observable<T> {
    private final Map<Consumer<? super T>, Set<SubscriptionOption>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService dispatcher = Executors.newCachedThreadPool();

    protected AbstractDispatchingObservable() {
    }

    @Override
    public Subscription subscribe(Consumer<? super T> listener, SubscriptionOption... options) {
        Set<SubscriptionOption> optionSet = new HashSet<>(Arrays.asList(options));
        if (optionSet.contains(WEAK)) {
            listeners.put(new WeakConsumer<>(listener, listeners::remove), optionSet);
        } else {
            listeners.put(listener, optionSet);
        }
        return new ObservableSubscription(listener);
    }

    protected void update(T newValue) {
        listeners.keySet().forEach(l -> dispatch(l, newValue));
    }

    protected void update(T newValue, Predicate<Set<SubscriptionOption>> optionPredicate) {
        listeners.entrySet().stream() //
                .filter(entry -> optionPredicate.test(entry.getValue())) //
                .map(Map.Entry::getKey) //
                .forEach(l -> dispatch(l, newValue));
    }

    private void dispatch(Consumer<? super T> listener, T value) {
        dispatcher.submit(() -> {
            try {
                listener.accept(value);
            } catch (Exception e) {
                System.err.println("Error in event handler\nvalue: " + value);
                e.printStackTrace();
            }
        });
    }


    private class ObservableSubscription implements Subscription {
        private final Consumer<? super T> listener;
        private ObservableSubscription(Consumer<? super T> listener) {
            this.listener = listener;
        }

        @Override
        public void unsubscribe() {
            listeners.remove(listener);
        }
    }
}
