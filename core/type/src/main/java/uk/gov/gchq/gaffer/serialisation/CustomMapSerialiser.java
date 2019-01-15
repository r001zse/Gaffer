/*
 * Copyright 2019 Crown Copyright
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
 */
package uk.gov.gchq.gaffer.serialisation;

import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.serialisation.implementation.JavaSerialiser;
import uk.gov.gchq.gaffer.serialisation.implementation.MapSerialiser;
import uk.gov.gchq.gaffer.types.CustomMap;

import java.io.Serializable;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CustomMapSerialiser implements ToBytesSerialiser<CustomMap> {
    private static final long serialVersionUID = 8028051359108072192L;

    @Override
    public boolean canHandle(final Class clazz) {
        return CustomMap.class.equals(clazz);
    }

    @Override
    public byte[] serialise(final CustomMap customMap) throws SerialisationException {
        try {
            return CustomMapInterim.serialise(customMap);
        } catch (Exception e) {
            throw new SerialisationException("Problem serialising CustomMap", e);
        }
    }

    @Override
    public CustomMap deserialise(final byte[] bytes) throws SerialisationException {
        try {
            return CustomMapInterim.deserialise(bytes);
        } catch (Exception e) {
            throw new SerialisationException("Problem serialising CustomMap", e);
        }
    }

    @Override
    public CustomMap deserialiseEmpty() throws SerialisationException {
        return null;
    }

    @Override
    public boolean preservesObjectOrdering() {
        return false;
    }

    @Override
    public boolean isConsistent() {
        return false;
    }

    private static class CustomMapInterim implements Serializable {
        private static final long serialVersionUID = 8084628302737276436L;
        private ToBytesSerialiser keySerialiser;
        private ToBytesSerialiser valueSerialiser;
        private byte[] byteMap;

        public CustomMapInterim(ToBytesSerialiser keySerialiser, ToBytesSerialiser valueSerialiser, byte[] byteMap) throws NullPointerException {
            requireNonNull(keySerialiser, "keySerialier can't be null");
            requireNonNull(valueSerialiser, "valueSerialiser can't be null");
            requireNonNull(byteMap, "byteMap can't be null");
            this.byteMap = byteMap;
            this.keySerialiser = keySerialiser;
            this.valueSerialiser = valueSerialiser;
        }

        public static byte[] serialise(final CustomMap customMap) throws SerialisationException {
            final MapSerialiser mapSerialiser = new MapSerialiser();
            assignKeySerialiser(customMap, mapSerialiser);
            assignValueSerialiser(customMap, mapSerialiser);
            final byte[] serialisedInnerMap = mapSerialiser.serialise(customMap.getMap());
            return serialise(customMap.getKeySerialiser(), customMap.getValueSerialiser(), serialisedInnerMap);
        }

        private static byte[] serialise(ToBytesSerialiser keySerialiser, ToBytesSerialiser valueSerialiser, byte[] map) throws SerialisationException {
            try {
                return serialise(new CustomMapInterim(keySerialiser, valueSerialiser, map));
            } catch (NullPointerException e) {
                throw new SerialisationException("Error creating CustomMapInterim for serialisation", e);
            }
        }

        private static byte[] serialise(final CustomMapInterim customMapInterim) throws SerialisationException {
            try {
                return new JavaSerialiser().serialise(customMapInterim);
            } catch (SerialisationException e) {
                throw new SerialisationException(String.format("Problem serialising %s via a interim java object using a %s", CustomMap.class.getSimpleName(), JavaSerialiser.class.getSimpleName()), e);
            }
        }

        public static CustomMap deserialise(byte[] bytes) throws SerialisationException {
            try {
                final CustomMapInterim mapInterim = (CustomMapInterim) new JavaSerialiser().deserialise(bytes);
                final Map<?, ?> innerMap = getInnerMap(mapInterim);

                return new CustomMap(mapInterim.getKeySerialiser(), mapInterim.getValueSerialiser(), innerMap);
            } catch (SerialisationException | ClassCastException e) {
                throw new SerialisationException("Problem deserialising bytes of interim object", e);
            }
        }

        protected static Map<?, ?> getInnerMap(final CustomMapInterim mapInterim) throws SerialisationException {
            final ToBytesSerialiser keySerialiser = mapInterim.getKeySerialiser();
            final ToBytesSerialiser valueSerialiser = mapInterim.getValueSerialiser();
            final byte[] byteMap = mapInterim.getByteMap();

            final MapSerialiser mapSerialiser = new MapSerialiser();
            mapSerialiser.setKeySerialiser(keySerialiser);
            mapSerialiser.setValueSerialiser(valueSerialiser);
            return mapSerialiser.deserialise(byteMap);
        }

        private static void assignValueSerialiser(final CustomMap customMap, final MapSerialiser mapSerialiser) {
            final ToBytesSerialiser valueSerialiser = customMap.getValueSerialiser();
            requireNonNull(valueSerialiser, "customMap.getValueSerialiser() is null");
            mapSerialiser.setValueSerialiser(valueSerialiser);
        }

        private static void assignKeySerialiser(final CustomMap customMap, final MapSerialiser mapSerialiser) {
            final ToBytesSerialiser keySerialiser = customMap.getKeySerialiser();
            requireNonNull(keySerialiser, "customMap.getKeySerialiser() is null");
            mapSerialiser.setKeySerialiser(keySerialiser);
        }

        public ToBytesSerialiser getKeySerialiser() {
            return keySerialiser;
        }

        public ToBytesSerialiser getValueSerialiser() {
            return valueSerialiser;
        }

        public byte[] getByteMap() {
            return byteMap;
        }
    }
}

