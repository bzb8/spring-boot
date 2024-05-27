/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A configuration property name composed of elements separated by dots. User created
 * names may contain the characters "{@code a-z}" "{@code 0-9}") and "{@code -}", they
 * must be lower-case and must start with an alphanumeric character. The "{@code -}" is
 * used purely for formatting, i.e. "{@code foo-bar}" and "{@code foobar}" are considered
 * equivalent.
 * <p>
 * 构建一个由点分隔的配置属性名称。用户创建的名称可以包含字符"{@code a-z}"、"{@code 0-9}"和"{@code -}"，
 * 它们必须是小写，并且必须以字母数字字符开头。字符"{@code -}"仅用于格式化，即"{@code foo-bar}"和"{@code foobar}"
 * 被认为是等价的。
 *
 * <p>
 * The "{@code [}" and "{@code ]}" characters may be used to indicate an associative
 * index(i.e. a {@link Map} key or a {@link Collection} index). Indexes names are not
 * restricted and are considered case-sensitive.
 * <p>
 * 字符"{@code [}"和"{@code ]}"可以用于指示关联索引（即{@link Map}的键或{@link Collection}的索引）。
 * 索引名称不受限制，并且被认为是大小写敏感的。
 *
 * <p>
 * Here are some typical examples:
 * <ul>
 * <li>{@code spring.main.banner-mode}</li>
 * <li>{@code server.hosts[0].name}</li>
 * <li>{@code log[org.springboot].level}</li>
 * </ul>
 * <p>
 * 以下是一些典型的例子：
 * <ul>
 * <li>{@code spring.main.banner-mode}</li>
 * <li>{@code server.hosts[0].name}</li>
 * <li>{@code log[org.springboot].level}</li>
 * </ul>
 *
 * <p>
 * ConfigurationPropertyName代表的是配置属性的名称，在Binder里的作用可以简单描述为：为Bindable的每个属性构造一个ConfigurationPropertyName，
 * 然后为.properties里的kv的每个key生成一个ConfigurationPropertyName，如果这俩equals，就把.properties里的值绑定到Bindable的这个属性上。
 * 所以ConfigurationPropertyName就有三个重要功能：
 * 1. 将一串字符串构造成ConfigurationPropertyName
 * 2. 判断不同ConfigurationPropertyName实例是否equal
 * 3. toString
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see #of(CharSequence)
 * @see ConfigurationPropertySource
 */
public final class ConfigurationPropertyName implements Comparable<ConfigurationPropertyName> {

	private static final String EMPTY_STRING = "";

	/**
	 * An empty {@link ConfigurationPropertyName}.
	 */
	public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(Elements.EMPTY);
	// 属性名称字符串解析后的elements
	private Elements elements;
	// 和elements大小一样, 只有a-z、0-9，没有’-’，只有小写
	private final CharSequence[] uniformElements;

	private String string;

	private int hashCode;

	private ConfigurationPropertyName(Elements elements) {
		this.elements = elements;
		this.uniformElements = new CharSequence[elements.getSize()];
	}

	/**
	 * Returns {@code true} if this {@link ConfigurationPropertyName} is empty.
	 * @return {@code true} if the name is empty
	 */
	public boolean isEmpty() {
		return this.elements.getSize() == 0;
	}

	/**
	 * Return if the last element in the name is indexed.
	 * @return {@code true} if the last element is indexed
	 */
	public boolean isLastElementIndexed() {
		int size = getNumberOfElements();
		return (size > 0 && isIndexed(size - 1));
	}

	/**
	 * Return {@code true} if any element in the name is indexed.
	 * @return if the element has one or more indexed elements
	 * @since 2.2.10
	 */
	public boolean hasIndexedElement() {
		for (int i = 0; i < getNumberOfElements(); i++) {
			if (isIndexed(i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return if the element in the name is indexed.
	 * @param elementIndex the index of the element
	 * @return {@code true} if the element is indexed
	 */
	boolean isIndexed(int elementIndex) {
		return this.elements.getType(elementIndex).isIndexed();
	}

	/**
	 * Return if the element in the name is indexed and numeric.
	 * @param elementIndex the index of the element
	 * @return {@code true} if the element is indexed and numeric
	 */
	public boolean isNumericIndex(int elementIndex) {
		return this.elements.getType(elementIndex) == ElementType.NUMERICALLY_INDEXED;
	}

	/**
	 * Return the last element in the name in the given form.
	 * @param form the form to return
	 * @return the last element
	 */
	public String getLastElement(Form form) {
		int size = getNumberOfElements();
		return (size != 0) ? getElement(size - 1, form) : EMPTY_STRING;
	}

	/**
	 * Return an element in the name in the given form.
	 * @param elementIndex the element index -- 要获取元素的索引。
	 * @param form the form to return
	 * 获取元素的形式，可以是原始形式（ORIGINAL）或带连字符的形式（DASHED）。
	 * @return the last element
	 */
	public String getElement(int elementIndex, Form form) {
		// 获取指定索引的元素和元素类型
		CharSequence element = this.elements.get(elementIndex);
		ElementType type = this.elements.getType(elementIndex);

		// 如果元素是索引类型的，则直接返回元素字符串
		if (type.isIndexed()) {
			return element.toString();
		}

		// 根据指定的形式和元素类型处理并返回元素
		if (form == Form.ORIGINAL) {
			// 如果元素类型不是非统一格式，则直接返回元素字符串
			if (type != ElementType.NON_UNIFORM) {
				return element.toString();
			}
			// 如果元素类型是非统一格式，则将其转换为原始形式后返回
			return convertToOriginalForm(element).toString();
		}

		if (form == Form.DASHED) {
			// 如果元素类型是统一或带连字符的，则直接返回元素字符串
			if (type == ElementType.UNIFORM || type == ElementType.DASHED) {
				return element.toString();
			}
			// 如果元素类型不是统一或带连字符的，则将其转换为带连字符形式后返回
			return convertToDashedElement(element).toString();
		}

		// 处理统一形式的元素
		CharSequence uniformElement = this.uniformElements[elementIndex];
		if (uniformElement == null) {
			// 如果尚未转换为统一形式，根据元素类型进行转换
			uniformElement = (type != ElementType.UNIFORM) ? convertToUniformElement(element) : element;
			this.uniformElements[elementIndex] = uniformElement.toString();
		}
		return uniformElement.toString();
	}

	private CharSequence convertToOriginalForm(CharSequence element) {
		return convertElement(element, false,
				(ch, i) -> ch == '_' || ElementsParser.isValidChar(Character.toLowerCase(ch), i));
	}

	private CharSequence convertToDashedElement(CharSequence element) {
		return convertElement(element, true, ElementsParser::isValidChar);
	}

	private CharSequence convertToUniformElement(CharSequence element) {
		return convertElement(element, true, (ch, i) -> ElementsParser.isAlphaNumeric(ch));
	}

	private CharSequence convertElement(CharSequence element, boolean lowercase, ElementCharPredicate filter) {
		StringBuilder result = new StringBuilder(element.length());
		for (int i = 0; i < element.length(); i++) { // 遍历元素的每个字符
			char ch = lowercase ? Character.toLowerCase(element.charAt(i)) : element.charAt(i);
			if (filter.test(ch, i)) {
				result.append(ch);
			}
		}
		return result;
	}

	/**
	 * Return the total number of elements in the name.
	 * @return the number of elements
	 */
	public int getNumberOfElements() {
		return this.elements.getSize();
	}

	/**
	 * Create a new {@link ConfigurationPropertyName} by appending the given suffix.
	 * @param suffix the elements to append
	 * @return a new {@link ConfigurationPropertyName}
	 * @throws InvalidConfigurationPropertyNameException if the result is not valid
	 */
	public ConfigurationPropertyName append(String suffix) {
		if (!StringUtils.hasLength(suffix)) {
			return this;
		}
		Elements additionalElements = probablySingleElementOf(suffix);
		return new ConfigurationPropertyName(this.elements.append(additionalElements));
	}

	/**
	 * Create a new {@link ConfigurationPropertyName} by appending the given suffix.
	 * @param suffix the elements to append
	 * @return a new {@link ConfigurationPropertyName}
	 * @since 2.5.0
	 */
	public ConfigurationPropertyName append(ConfigurationPropertyName suffix) {
		if (suffix == null) {
			return this;
		}
		return new ConfigurationPropertyName(this.elements.append(suffix.elements));
	}

	/**
	 * Return the parent of this {@link ConfigurationPropertyName} or
	 * {@link ConfigurationPropertyName#EMPTY} if there is no parent.
	 * @return the parent name
	 */
	public ConfigurationPropertyName getParent() {
		int numberOfElements = getNumberOfElements();
		return (numberOfElements <= 1) ? EMPTY : chop(numberOfElements - 1);
	}

	/**
	 * Return a new {@link ConfigurationPropertyName} by chopping this name to the given
	 * {@code size}. For example, {@code chop(1)} on the name {@code foo.bar} will return
	 * {@code foo}.
	 * @param size the size to chop
	 * @return the chopped name
	 */
	public ConfigurationPropertyName chop(int size) {
		if (size >= getNumberOfElements()) {
			return this;
		}
		return new ConfigurationPropertyName(this.elements.chop(size));
	}

	/**
	 * Return a new {@link ConfigurationPropertyName} by based on this name offset by
	 * specific element index. For example, {@code chop(1)} on the name {@code foo.bar}
	 * will return {@code bar}.
	 * @param offset the element offset
	 * @return the sub name
	 * @since 2.5.0
	 */
	public ConfigurationPropertyName subName(int offset) {
		if (offset == 0) {
			return this;
		}
		if (offset == getNumberOfElements()) {
			return EMPTY;
		}
		if (offset < 0 || offset > getNumberOfElements()) {
			throw new IndexOutOfBoundsException("Offset: " + offset + ", NumberOfElements: " + getNumberOfElements());
		}
		return new ConfigurationPropertyName(this.elements.subElements(offset));
	}

	/**
	 * Returns {@code true} if this element is an immediate parent of the specified name.
	 * @param name the name to check
	 * @return {@code true} if this name is an ancestor
	 */
	public boolean isParentOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		if (getNumberOfElements() != name.getNumberOfElements() - 1) {
			return false;
		}
		return isAncestorOf(name);
	}

	/**
	 * Returns {@code true} if this element is an ancestor (immediate or nested parent) of
	 * the specified name.
	 * @param name the name to check
	 * @return {@code true} if this name is an ancestor
	 */
	public boolean isAncestorOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		if (getNumberOfElements() >= name.getNumberOfElements()) {
			return false;
		}
		return elementsEqual(name);
	}

	@Override
	public int compareTo(ConfigurationPropertyName other) {
		return compare(this, other);
	}

	private int compare(ConfigurationPropertyName n1, ConfigurationPropertyName n2) {
		int l1 = n1.getNumberOfElements();
		int l2 = n2.getNumberOfElements();
		int i1 = 0;
		int i2 = 0;
		while (i1 < l1 || i2 < l2) {
			try {
				ElementType type1 = (i1 < l1) ? n1.elements.getType(i1) : null;
				ElementType type2 = (i2 < l2) ? n2.elements.getType(i2) : null;
				String e1 = (i1 < l1) ? n1.getElement(i1++, Form.UNIFORM) : null;
				String e2 = (i2 < l2) ? n2.getElement(i2++, Form.UNIFORM) : null;
				int result = compare(e1, type1, e2, type2);
				if (result != 0) {
					return result;
				}
			}
			catch (ArrayIndexOutOfBoundsException ex) {
				throw new RuntimeException(ex);
			}
		}
		return 0;
	}

	private int compare(String e1, ElementType type1, String e2, ElementType type2) {
		if (e1 == null) {
			return -1;
		}
		if (e2 == null) {
			return 1;
		}
		int result = Boolean.compare(type2.isIndexed(), type1.isIndexed());
		if (result != 0) {
			return result;
		}
		if (type1 == ElementType.NUMERICALLY_INDEXED && type2 == ElementType.NUMERICALLY_INDEXED) {
			long v1 = Long.parseLong(e1);
			long v2 = Long.parseLong(e2);
			return Long.compare(v1, v2);
		}
		return e1.compareTo(e2);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
		if (getNumberOfElements() != other.getNumberOfElements()) {
			return false;
		}
		if (this.elements.canShortcutWithSource(ElementType.UNIFORM)
				&& other.elements.canShortcutWithSource(ElementType.UNIFORM)) {
			return toString().equals(other.toString());
		}
		return elementsEqual(other);
	}

	private boolean elementsEqual(ConfigurationPropertyName name) {
		for (int i = this.elements.getSize() - 1; i >= 0; i--) {
			if (elementDiffers(this.elements, name.elements, i)) {
				return false;
			}
		}
		return true;
	}

	private boolean elementDiffers(Elements e1, Elements e2, int i) {
		ElementType type1 = e1.getType(i);
		ElementType type2 = e2.getType(i);
		if (type1.allowsFastEqualityCheck() && type2.allowsFastEqualityCheck()) {
			return !fastElementEquals(e1, e2, i);
		}
		if (type1.allowsDashIgnoringEqualityCheck() && type2.allowsDashIgnoringEqualityCheck()) {
			return !dashIgnoringElementEquals(e1, e2, i);
		}
		return !defaultElementEquals(e1, e2, i);
	}

	private boolean fastElementEquals(Elements e1, Elements e2, int i) {
		int length1 = e1.getLength(i);
		int length2 = e2.getLength(i);
		if (length1 == length2) {
			int i1 = 0;
			while (length1-- != 0) {
				char ch1 = e1.charAt(i, i1);
				char ch2 = e2.charAt(i, i1);
				if (ch1 != ch2) {
					return false;
				}
				i1++;
			}
			return true;
		}
		return false;
	}

	private boolean dashIgnoringElementEquals(Elements e1, Elements e2, int i) {
		int l1 = e1.getLength(i);
		int l2 = e2.getLength(i);
		int i1 = 0;
		int i2 = 0;
		while (i1 < l1) {
			if (i2 >= l2) {
				return remainderIsDashes(e1, i, i1);
			}
			char ch1 = e1.charAt(i, i1);
			char ch2 = e2.charAt(i, i2);
			if (ch1 == '-') {
				i1++;
			}
			else if (ch2 == '-') {
				i2++;
			}
			else if (ch1 != ch2) {
				return false;
			}
			else {
				i1++;
				i2++;
			}
		}
		if (i2 < l2) {
			if (e2.getType(i).isIndexed()) {
				return false;
			}
			do {
				char ch2 = e2.charAt(i, i2++);
				if (ch2 != '-') {
					return false;
				}
			}
			while (i2 < l2);
		}
		return true;
	}

	private boolean defaultElementEquals(Elements e1, Elements e2, int i) {
		int l1 = e1.getLength(i);
		int l2 = e2.getLength(i);
		boolean indexed1 = e1.getType(i).isIndexed();
		boolean indexed2 = e2.getType(i).isIndexed();
		int i1 = 0;
		int i2 = 0;
		while (i1 < l1) {
			if (i2 >= l2) {
				return remainderIsNotAlphanumeric(e1, i, i1);
			}
			char ch1 = indexed1 ? e1.charAt(i, i1) : Character.toLowerCase(e1.charAt(i, i1));
			char ch2 = indexed2 ? e2.charAt(i, i2) : Character.toLowerCase(e2.charAt(i, i2));
			if (!indexed1 && !ElementsParser.isAlphaNumeric(ch1)) {
				i1++;
			}
			else if (!indexed2 && !ElementsParser.isAlphaNumeric(ch2)) {
				i2++;
			}
			else if (ch1 != ch2) {
				return false;
			}
			else {
				i1++;
				i2++;
			}
		}
		if (i2 < l2) {
			return remainderIsNotAlphanumeric(e2, i, i2);
		}
		return true;
	}

	private boolean remainderIsNotAlphanumeric(Elements elements, int element, int index) {
		if (elements.getType(element).isIndexed()) {
			return false;
		}
		int length = elements.getLength(element);
		do {
			char c = Character.toLowerCase(elements.charAt(element, index++));
			if (ElementsParser.isAlphaNumeric(c)) {
				return false;
			}
		}
		while (index < length);
		return true;
	}

	private boolean remainderIsDashes(Elements elements, int element, int index) {
		if (elements.getType(element).isIndexed()) {
			return false;
		}
		int length = elements.getLength(element);
		do {
			char c = Character.toLowerCase(elements.charAt(element, index++));
			if (c != '-') {
				return false;
			}
		}
		while (index < length);
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = this.hashCode;
		Elements elements = this.elements;
		if (hashCode == 0 && elements.getSize() != 0) {
			for (int elementIndex = 0; elementIndex < elements.getSize(); elementIndex++) {
				int elementHashCode = 0;
				boolean indexed = elements.getType(elementIndex).isIndexed();
				int length = elements.getLength(elementIndex);
				for (int i = 0; i < length; i++) {
					char ch = elements.charAt(elementIndex, i);
					if (!indexed) {
						ch = Character.toLowerCase(ch);
					}
					if (ElementsParser.isAlphaNumeric(ch)) {
						elementHashCode = 31 * elementHashCode + ch;
					}
				}
				hashCode = 31 * hashCode + elementHashCode;
			}
			this.hashCode = hashCode;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		if (this.string == null) {
			this.string = buildToString();
		}
		return this.string;
	}

	private String buildToString() {
		if (this.elements.canShortcutWithSource(ElementType.UNIFORM, ElementType.DASHED)) {
			return this.elements.getSource().toString();
		}
		int elements = getNumberOfElements();
		StringBuilder result = new StringBuilder(elements * 8);
		for (int i = 0; i < elements; i++) {
			boolean indexed = isIndexed(i);
			if (result.length() > 0 && !indexed) {
				result.append('.');
			}
			if (indexed) {
				result.append('[');
				result.append(getElement(i, Form.ORIGINAL));
				result.append(']');
			}
			else {
				result.append(getElement(i, Form.DASHED));
			}
		}
		return result.toString();
	}

	/**
	 * Returns if the given name is valid. If this method returns {@code true} then the
	 * name may be used with {@link #of(CharSequence)} without throwing an exception.
	 * @param name the name to test
	 * @return {@code true} if the name is valid
	 */
	public static boolean isValid(CharSequence name) {
		return of(name, true) != null;
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string.
	 * 根据指定的字符串返回一个 {@link ConfigurationPropertyName} 对象。
	 * <p>此方法用于将传入的字符序列转换为配置属性名称对象。转换过程中会校验名称的有效性，
	 * 如果名称不符合要求，则会抛出 {@link InvalidConfigurationPropertyNameException} 异常。
	 *
	 * @param name the source name
	 * @return a {@link ConfigurationPropertyName} instance
	 * @throws InvalidConfigurationPropertyNameException if the name is not valid
	 */
	public static ConfigurationPropertyName of(CharSequence name) {
		return of(name, false);
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string or {@code null}
	 * if the name is not valid.
	 * @param name the source name
	 * @return a {@link ConfigurationPropertyName} instance
	 * @since 2.3.1
	 */
	public static ConfigurationPropertyName ofIfValid(CharSequence name) {
		return of(name, true);
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string.
	 * @param name the source name
	 * @param returnNullIfInvalid if null should be returned if the name is not valid
	 * @return a {@link ConfigurationPropertyName} instance
	 * @throws InvalidConfigurationPropertyNameException if the name is not valid and
	 * {@code returnNullIfInvalid} is {@code false}
	 */
	static ConfigurationPropertyName of(CharSequence name, boolean returnNullIfInvalid) {
		Elements elements = elementsOf(name, returnNullIfInvalid);
		return (elements != null) ? new ConfigurationPropertyName(elements) : null;
	}

	private static Elements probablySingleElementOf(CharSequence name) {
		return elementsOf(name, false, 1);
	}

	private static Elements elementsOf(CharSequence name, boolean returnNullIfInvalid) {
		return elementsOf(name, returnNullIfInvalid, ElementsParser.DEFAULT_CAPACITY);
	}

	/**
	 * 根据给定的名称序列解析其元素。
	 *
	 * @param name 待解析的名称序列，不能为空。
	 * @param returnNullIfInvalid 如果名称无效且此参数为true，则返回null而不是抛出异常。
	 * @param parserCapacity 解析器容量，用于控制解析过程中的内存使用。
	 * @return 解析后的元素集合，如果名称无效且returnNullIfInvalid为true，则返回null。
	 * @throws InvalidConfigurationPropertyNameException 如果名称以点开头或结尾，或包含不一致的字符，则抛出此异常。
	 */
	private static Elements elementsOf(CharSequence name, boolean returnNullIfInvalid, int parserCapacity) {
		// 检查名称是否为null
		if (name == null) {
			Assert.isTrue(returnNullIfInvalid, "Name must not be null");
			return null;
		}
		// 空名称直接返回空元素集合
		if (name.length() == 0) {
			return Elements.EMPTY;
		}
		// 检查名称首尾字符是否为点
		if (name.charAt(0) == '.' || name.charAt(name.length() - 1) == '.') {
			if (returnNullIfInvalid) {
				return null;
			}
			throw new InvalidConfigurationPropertyNameException(name, Collections.singletonList('.'));
		}
		// 解析名称序列
		Elements elements = new ElementsParser(name, '.', parserCapacity).parse();
		// 验证解析后的每个元素是否一致
		for (int i = 0; i < elements.getSize(); i++) {
			if (elements.getType(i) == ElementType.NON_UNIFORM) {
				if (returnNullIfInvalid) {
					return null;
				}
				throw new InvalidConfigurationPropertyNameException(name, getInvalidChars(elements, i));
			}
		}
		return elements;
	}

	private static List<Character> getInvalidChars(Elements elements, int index) {
		List<Character> invalidChars = new ArrayList<>();
		for (int charIndex = 0; charIndex < elements.getLength(index); charIndex++) {
			char ch = elements.charAt(index, charIndex);
			if (!ElementsParser.isValidChar(ch, charIndex)) {
				invalidChars.add(ch);
			}
		}
		return invalidChars;
	}

	/**
	 * Create a {@link ConfigurationPropertyName} by adapting the given source. See
	 * {@link #adapt(CharSequence, char, Function)} for details.
	 * <p>
	 * 将给定的源名称适配为一个{@link ConfigurationPropertyName}对象。有关详细信息，请参阅
	 * {@link #adapt(CharSequence, char, Function)}方法。
	 *
	 * @param name the name to parse
	 * 需要解析的名称。
	 * @param separator the separator used to split the name
	 * 用于分割名称的分隔符。
	 * @return a {@link ConfigurationPropertyName}
	 */
	public static ConfigurationPropertyName adapt(CharSequence name, char separator) {
		return adapt(name, separator, null);
	}

	/**
	 * Create a {@link ConfigurationPropertyName} by adapting the given source. The name
	 * is split into elements around the given {@code separator}. This method is more
	 * lenient than {@link #of} in that it allows mixed case names and '{@code _}'
	 * characters. Other invalid characters are stripped out during parsing.
	 * <p>
	 * 根据给定的源创建一个 {@link ConfigurationPropertyName}。该名称将根据给定的 {@code separator} 进行分割。
	 * 此方法比 {@link #of} 更为宽松，允许混合大小写名称和 '{@code _}' 字符。解析过程中会去除其他无效字符。
	 *
	 * <p>
	 * The {@code elementValueProcessor} function may be used if additional processing is
	 * required on the extracted element values.
	 * <p>
	 * 可以使用 {@code elementValueProcessor} 函数对提取的元素值进行额外处理。
	 *
	 * @param name the name to parse
	 * @param separator the separator used to split the name
	 * 用于分割名称的字符
	 * @param elementValueProcessor a function to process element values
	 * 对元素值进行处理的函数
	 * @return a {@link ConfigurationPropertyName}
	 */
	static ConfigurationPropertyName adapt(CharSequence name, char separator,
			Function<CharSequence, CharSequence> elementValueProcessor) {
		Assert.notNull(name, "Name must not be null");
		if (name.length() == 0) {
			return EMPTY;
		}
		Elements elements = new ElementsParser(name, separator).parse(elementValueProcessor);
		if (elements.getSize() == 0) {
			return EMPTY;
		}
		return new ConfigurationPropertyName(elements);
	}

	/**
	 * The various forms that a non-indexed element value can take. 非索引元素值可以采用的各种形式。
	 */
	public enum Form {

		/**
		 * The original form as specified when the name was created or adapted. For
		 * example: 定义了原始形式，转换为字符串时保持不变
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
		 * <li>"{@code fooBar}" = "{@code fooBar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foo_bar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		ORIGINAL,

		/**
		 * The dashed configuration form (used for toString; lower-case with only
		 * alphanumeric characters and dashes).  // 定义了使用连字符分隔的格式，用于转换为字符串，只包含小写字母、数字和连字符
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		DASHED,

		/**
		 * The uniform configuration form (used for equals/hashCode; lower-case with only
		 * alphanumeric characters). // 定义了一致的格式，用于相等性和哈希码计算，只包含小写字母和数字
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foobar}"</li>
		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		UNIFORM

	}

	/**
	 * Allows access to the individual elements that make up the name. We store the
	 * indexes in arrays rather than a list of object in order to conserve memory.
	 * <p>
	 * Elements类是一个用于存储名称组成元素的内部类。
	 * 通过使用数组存储索引来节省内存，而不是使用对象列表。
	 *
	 * <p>
	 * Elements封装了配置属性名称的层级化结构
	 * 看ElementsParser
	 */
	private static class Elements {

		private static final int[] NO_POSITION = {};

		private static final ElementType[] NO_TYPE = {};

		public static final Elements EMPTY = new Elements("", 0, NO_POSITION, NO_POSITION, NO_TYPE, null);
		//
		private final CharSequence source;
		// 有多少个element
		private final int size;
		// Elements使用数组保存配置属性的每个element的开始index、结束index, 貌似遇见[]最多两层
		private final int[] start;

		private final int[] end;

		private final ElementType[] type;

		/**
		 * Contains any resolved elements or can be {@code null} if there aren't any.
		 * Resolved elements allow us to modify the element values in some way (or example
		 * when adapting with a mapping function, or when append has been called). Note
		 * that this array is not used as a cache, in fact, when it's not null then
		 * {@link #canShortcutWithSource} will always return false which may hurt
		 * performance.
		 * 此字段用于存储已解析的元素数组，或者如果没有任何解析元素，则可以为null。
		 * 已解析的元素允许我们以某种方式修改元素的值（例如，当使用映射函数进行适配时，或者在调用append之后）。
		 * 请注意，这个数组并不用作缓存。事实上，当它不为null时，{@link #canShortcutWithSource} 方法将始终返回false，
		 * 这可能会影响性能。
		 */
		private final CharSequence[] resolved;

		Elements(CharSequence source, int size, int[] start, int[] end, ElementType[] type, CharSequence[] resolved) {
			super();
			this.source = source;
			this.size = size;
			this.start = start;
			this.end = end;
			this.type = type;
			this.resolved = resolved;
		}

		Elements append(Elements additional) {
			int size = this.size + additional.size;
			// 扩容后的新数组
			ElementType[] type = new ElementType[size];
			// 复制当前type到新数组的起始位置
			System.arraycopy(this.type, 0, type, 0, this.size);
			// 复制additional的type到新数组的size位置
			System.arraycopy(additional.type, 0, type, this.size, additional.size);
			CharSequence[] resolved = newResolved(size);
			for (int i = 0; i < additional.size; i++) {
				resolved[this.size + i] = additional.get(i);
			}
			return new Elements(this.source, size, this.start, this.end, type, resolved);
		}

		Elements chop(int size) {
			CharSequence[] resolved = newResolved(size);
			return new Elements(this.source, size, this.start, this.end, this.type, resolved);
		}

		Elements subElements(int offset) {
			int size = this.size - offset;
			CharSequence[] resolved = newResolved(size);
			int[] start = new int[size];
			System.arraycopy(this.start, offset, start, 0, size);
			int[] end = new int[size];
			System.arraycopy(this.end, offset, end, 0, size);
			ElementType[] type = new ElementType[size];
			System.arraycopy(this.type, offset, type, 0, size);
			return new Elements(this.source, size, start, end, type, resolved);
		}

		private CharSequence[] newResolved(int size) {
			CharSequence[] resolved = new CharSequence[size];
			if (this.resolved != null) {
				System.arraycopy(this.resolved, 0, resolved, 0, Math.min(size, this.size));
			}
			return resolved;
		}

		int getSize() {
			return this.size;
		}

		CharSequence get(int index) { // 从source中截取相应索引范围的CharSequence
			if (this.resolved != null && this.resolved[index] != null) { // 优先使用解析后的结果
				return this.resolved[index];
			}
			int start = this.start[index];
			int end = this.end[index];
			return this.source.subSequence(start, end);
		}

		int getLength(int index) {
			if (this.resolved != null && this.resolved[index] != null) {
				return this.resolved[index].length();
			}
			int start = this.start[index];
			int end = this.end[index];
			return end - start;
		}

		char charAt(int index, int charIndex) {
			if (this.resolved != null && this.resolved[index] != null) {
				return this.resolved[index].charAt(charIndex);
			}
			int start = this.start[index];
			return this.source.charAt(start + charIndex);
		}

		ElementType getType(int index) {
			return this.type[index];
		}

		CharSequence getSource() {
			return this.source;
		}

		/**
		 * Returns if the element source can be used as a shortcut for an operation such
		 * as {@code equals} or {@code toString}.
		 * @param requiredType the required type
		 * @return {@code true} if all elements match at least one of the types
		 */
		boolean canShortcutWithSource(ElementType requiredType) {
			return canShortcutWithSource(requiredType, requiredType);
		}

		/**
		 * Returns if the element source can be used as a shortcut for an operation such
		 * as {@code equals} or {@code toString}.
		 * @param requiredType the required type
		 * @param alternativeType and alternative required type
		 * @return {@code true} if all elements match at least one of the types
		 */
		boolean canShortcutWithSource(ElementType requiredType, ElementType alternativeType) {
			if (this.resolved != null) {
				return false;
			}
			for (int i = 0; i < this.size; i++) {
				ElementType type = this.type[i];
				if (type != requiredType && type != alternativeType) {
					return false;
				}
				if (i > 0 && this.end[i - 1] + 1 != this.start[i]) {
					return false;
				}
			}
			return true;
		}

	}

	/**
	 * Main parsing logic used to convert a {@link CharSequence} to {@link Elements}.
	 * 主要的解析逻辑，用于将{@link CharSequence}转换为{@link Elements}。
	 * 这是一个内部类，提供了具体的解析实现。
	 */
	private static class ElementsParser {

		private static final int DEFAULT_CAPACITY = 6;
		// 原始属性名称字符串
		private final CharSequence source;

		private final char separator;
		// 有多少个element
		private int size;
		// start end type resolved 数组大小一致，下标对应，分隔符分割后的element。遇见[或者.号，就添加元素。含头不含尾
		private int[] start;

		private int[] end;

		private ElementType[] type;
		// valueProcessor 解析后的value，并再次parse()之后的值
		private CharSequence[] resolved;

		ElementsParser(CharSequence source, char separator) {
			this(source, separator, DEFAULT_CAPACITY);
		}

		ElementsParser(CharSequence source, char separator, int capacity) {
			this.source = source;
			this.separator = separator;
			this.start = new int[capacity];
			this.end = new int[capacity];
			this.type = new ElementType[capacity];
		}

		Elements parse() {
			return parse(null);
		}

		/**
		 * 解析源字符串，根据指定的规则将字符串分割成多个元素，并通过valueProcessor函数对每个元素进行处理。
		 *
		 * @param valueProcessor 一个函数，用于处理每个分割出来的元素。它接受一个CharSequence类型的参数，并返回一个CharSequence类型的结果。
		 * @return Elements对象，包含了解析后的元素集合。
		 */
		Elements parse(Function<CharSequence, CharSequence> valueProcessor) {
			// 初始化源字符串长度 spring.bo-ot
			int length = this.source.length();
			// 开括号计数
			int openBracketCount = 0;
			// 起始索引，每个元素的起始索引
			int start = 0;
			// 元素类型
			ElementType type = ElementType.EMPTY;

			// 遍历源字符串
			for (int i = 0; i < length; i++) {
				// 当前处理的字符
				char ch = this.source.charAt(i);
				if (ch == '[') {
					// 遇到第一个开括号，更新起始索引和元素类型，开始记录数值索引的元素
					if (openBracketCount == 0) {
						add(start, i, type, valueProcessor); // 添加上一个元素
						start = i + 1; // 更新起始索引 + 1，表示下一个元素的起始索引
						type = ElementType.NUMERICALLY_INDEXED;
					}
					openBracketCount++;
				}
				else if (ch == ']') {
					// 遇到闭括号，减少开括号计数，若开闭括号匹配，则更新起始索引和元素类型为空
					openBracketCount--;
					if (openBracketCount == 0) {
						add(start, i, type, valueProcessor);
						start = i + 1;
						type = ElementType.EMPTY;
					}
				}
				else if (!type.isIndexed() && ch == this.separator) { // 非下标索引元素且遇到分隔符，start数组等增加
					// 非数值索引元素且遇到分隔符，更新起始索引和元素类型为空
					add(start, i, type, valueProcessor);
					start = i + 1; // 更新起始索引 + 1，表示下一个元素的起始索引
					type = ElementType.EMPTY; // type = ElementType.EMPTY;
				}
				else {
					// 更新元素类型
					type = updateType(type, ch, i - start);
				}
			}

			// 若存在未匹配的开括号，则将元素类型设置为非统一类型
			if (openBracketCount != 0) {
				type = ElementType.NON_UNIFORM;
			}

			// 添加最后一个元素并返回解析结果
			add(start, length, type, valueProcessor);
			return new Elements(this.source, this.size, this.start, this.end, this.type, this.resolved);
		}

		private ElementType updateType(ElementType existingType, char ch, int index) {
			if (existingType.isIndexed()) {
				if (existingType == ElementType.NUMERICALLY_INDEXED && !isNumeric(ch)) {
					return ElementType.INDEXED;
				}
				return existingType;
			}
			if (existingType == ElementType.EMPTY && isValidChar(ch, index)) {
				return (index == 0) ? ElementType.UNIFORM : ElementType.NON_UNIFORM; // index == 0 下一个元素的起始位置
			}
			if (existingType == ElementType.UNIFORM && ch == '-') {
				return ElementType.DASHED;
			}
			if (!isValidChar(ch, index)) {
				if (existingType == ElementType.EMPTY && !isValidChar(Character.toLowerCase(ch), index)) {
					return ElementType.EMPTY;
				}
				return ElementType.NON_UNIFORM;
			}
			return existingType;
		}

		/**
		 * 向数据结构中添加一个元素范围及其类型和处理后的值。
		 *
		 * @param start 元素开始的位置。
		 * @param end 元素结束的位置。
		 * @param type 元素的类型。
		 * @param valueProcessor 对元素内容进行处理的函数，可以为null。
		 * 当不为null时，会使用此函数处理元素的内容，并根据处理结果更新元素的类型和值。
		 */
		private void add(int start, int end, ElementType type, Function<CharSequence, CharSequence> valueProcessor) {
			// 如果范围无效（长度小于1）或类型为EMPTY，则不进行添加
			if ((end - start) < 1 || type == ElementType.EMPTY) {
				return;
			}
			// 如果数组已满，则扩展数组容量
			if (this.start.length == this.size) {
				this.start = expand(this.start);
				this.end = expand(this.end);
				this.type = expand(this.type);
				this.resolved = expand(this.resolved);
			}
			// 如果提供了valueProcessor，并且resolved数组未初始化，则初始化resolved数组
			if (valueProcessor != null) {
				if (this.resolved == null) {
					this.resolved = new CharSequence[this.start.length];
				}
				// 使用valueProcessor处理元素内容，解析处理后的结果
				CharSequence resolved = valueProcessor.apply(this.source.subSequence(start, end));
				// 再次解析
				Elements resolvedElements = new ElementsParser(resolved, '.').parse();
				// 确保解析结果只包含单个元素
				Assert.state(resolvedElements.getSize() == 1, "Resolved element must not contain multiple elements");
				this.resolved[this.size] = resolvedElements.get(0);
				type = resolvedElements.getType(0);
			}
			// 添加元素的开始位置、结束位置、类型到对应的数组，并更新元素数量
			this.start[this.size] = start;
			this.end[this.size] = end;
			this.type[this.size] = type;
			this.size++;
		}

		private int[] expand(int[] src) {
			int[] dest = new int[src.length + DEFAULT_CAPACITY];
			System.arraycopy(src, 0, dest, 0, src.length);
			return dest;
		}

		private ElementType[] expand(ElementType[] src) {
			ElementType[] dest = new ElementType[src.length + DEFAULT_CAPACITY];
			System.arraycopy(src, 0, dest, 0, src.length);
			return dest;
		}

		private CharSequence[] expand(CharSequence[] src) {
			if (src == null) {
				return null;
			}
			CharSequence[] dest = new CharSequence[src.length + DEFAULT_CAPACITY];
			System.arraycopy(src, 0, dest, 0, src.length);
			return dest;
		}

		static boolean isValidChar(char ch, int index) { // a-z, 0-9, -
			return isAlpha(ch) || isNumeric(ch) || (index != 0 && ch == '-');
		}

		static boolean isAlphaNumeric(char ch) {
			return isAlpha(ch) || isNumeric(ch);
		}

		private static boolean isAlpha(char ch) {
			return ch >= 'a' && ch <= 'z';
		}

		private static boolean isNumeric(char ch) {
			return ch >= '0' && ch <= '9';
		}

	}

	/**
	 * The various types of element that we can detect.
	 * <p>
	 * 定义可检测到的元素类型的枚举。
	 */
	private enum ElementType {

		/**
		 * The element is logically empty (contains no valid chars).
		 * 元素逻辑上为空（不包含有效字符）。
		 */
		EMPTY(false),

		/**
		 * The element is a uniform name (a-z, 0-9, no dashes, lowercase).
		 * 只有a-z、0-9，没有’-’，只有小写
		 */
		UNIFORM(false),

		/**
		 * The element is almost uniform, but it contains (but does not start with) at
		 * least one dash.
		 * 跟UNIFORM一样，只是包含’-’
		 */
		DASHED(false),

		/**
		 * The element contains non-uniform characters and will need to be converted.
		 * 可能存在大写，或者有特殊字符
		 */
		NON_UNIFORM(false),

		/**
		 * The element is non-numerically indexed.
		 * 存在非数字下标，比如[a]
		 *
		 */
		INDEXED(true),

		/**
		 * The element is numerically indexed.
		 * 存在数字下标，比如[0]
		 */
		NUMERICALLY_INDEXED(true);

		// indexed属性表示是否数组或map下标类型
		private final boolean indexed;

		ElementType(boolean indexed) {
			this.indexed = indexed;
		}

		public boolean isIndexed() {
			return this.indexed;
		}

		public boolean allowsFastEqualityCheck() {
			return this == UNIFORM || this == NUMERICALLY_INDEXED;
		}

		public boolean allowsDashIgnoringEqualityCheck() {
			return allowsFastEqualityCheck() || this == DASHED;
		}

	}

	/**
	 * Predicate used to filter element chars.
	 */
	private interface ElementCharPredicate {

		boolean test(char ch, int index);

	}

}
