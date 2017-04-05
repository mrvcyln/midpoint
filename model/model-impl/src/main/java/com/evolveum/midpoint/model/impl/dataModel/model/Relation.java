/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.dataModel.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author mederly
 */
public class Relation {

	@NotNull final private List<DataItem> sources;
	@Nullable final private DataItem target;

	public Relation(@NotNull List<DataItem> sources, @Nullable DataItem target) {
		this.sources = sources;
		this.target = target;
	}

	@NotNull
	public List<DataItem> getSources() {
		return sources;
	}

	@Nullable
	public DataItem getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "Relation{" +
				"sources=" + sources +
				", target=" + target +
				'}';
	}

}
