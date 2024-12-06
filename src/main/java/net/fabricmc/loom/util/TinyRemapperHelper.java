/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.util;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

public class TinyRemapperHelper {
	public static IMappingProvider create(MappingTree mappings, String from, String to, boolean remapLocalVariables) {
		return (acceptor) -> {
			final int fromId = mappings.getNamespaceId(from);
			final int toId = mappings.getNamespaceId(to);

			for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
				String className = classDef.getName(fromId);

				if (className == null) {
					continue;
				}

				String dstClassName = classDef.getName(toId);

				if (dstClassName == null) {
					// Unsure if this is correct, should be better than crashing tho.
					dstClassName = className;
				}

				acceptor.acceptClass(className, dstClassName);

				for (MappingTree.FieldMapping field : classDef.getFields()) {
					String fieldName = field.getName(fromId);

					if (fieldName == null) {
						continue;
					}

					String dstFieldName = field.getName(toId);

					if (dstFieldName == null) {
						dstFieldName = fieldName;
					}

					acceptor.acceptField(memberOf(className, fieldName, field.getDesc(fromId)), dstFieldName);
				}

				for (MappingTree.MethodMapping method : classDef.getMethods()) {
					String methodName = method.getName(fromId);

					if (methodName == null) {
						continue;
					}

					String dstMethodName = method.getName(toId);

					if (dstMethodName == null) {
						dstMethodName = methodName;
					}

					IMappingProvider.Member methodIdentifier = memberOf(className, methodName, method.getDesc(fromId));
					acceptor.acceptMethod(methodIdentifier, dstMethodName);

					if (remapLocalVariables) {
						for (MappingTree.MethodArgMapping parameter : method.getArgs()) {
							String name = parameter.getName(toId);

							if (name == null) {
								continue;
							}

							acceptor.acceptMethodArg(methodIdentifier, parameter.getLvIndex(), name);
						}

						for (MappingTree.MethodVarMapping localVariable : method.getVars()) {
							acceptor.acceptMethodVar(methodIdentifier, localVariable.getLvIndex(),
								localVariable.getStartOpIdx(), localVariable.getLvtRowIndex(),
								localVariable.getName(toId));
						}
					}
				}
			}
		};
	}

	private static IMappingProvider.Member memberOf(String className, String memberName, String descriptor) {
		return new IMappingProvider.Member(className, memberName, descriptor);
	}
}
