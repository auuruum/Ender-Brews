/**
 * Keeps the code for one Minecraft version out of a source file that serves several:
 * <pre>
 * //#if >=1.21.6
 * graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ...);
 * //#elif >=1.21.2 <1.21.5
 * ...
 * //#else
 * ...
 * //#endif
 * </pre>
 * Every condition term must hold. Lines that are left out and the directives themselves become
 * empty lines, so compiler errors keep pointing at the right line. Use one instance per file.
 *
 * <p>Renames that touch many lines are done here instead: from 1.21.11 on, Mojang calls
 * ResourceLocation Identifier (and ResourceKey.location() identifier()).
 */
class Preprocessor {

    private static final Map<String, String> RENAMES_1_21_11 = [
            'ResourceLocation': 'Identifier',
            '.location()'     : '.identifier()',
    ]

    private final List<Integer> version
    // One entry per open //#if: [active, an earlier branch was taken]
    private final List<boolean[]> stack = []

    Preprocessor(String minecraftVersion) {
        version = parse(minecraftVersion)
    }

    String line(String line) {
        String trimmed = line.trim()
        if (trimmed.startsWith('//#if ')) {
            boolean cond = matches(trimmed.substring(6))
            stack.add([active() && cond, cond] as boolean[])
            return ''
        }
        if (trimmed.startsWith('//#elif ')) {
            boolean[] top = top(trimmed)
            boolean cond = !top[1] && matches(trimmed.substring(8))
            top[0] = parentActive() && cond
            top[1] |= cond
            return ''
        }
        if (trimmed == '//#else') {
            boolean[] top = top(trimmed)
            top[0] = parentActive() && !top[1]
            top[1] = true
            return ''
        }
        if (trimmed == '//#endif') {
            top(trimmed)
            stack.remove(stack.size() - 1)
            return ''
        }
        if (trimmed.startsWith('//#')) {
            throw new IllegalArgumentException("Unknown directive: $trimmed")
        }
        if (!active()) {
            return ''
        }
        if (compare(version, [1, 21, 11]) >= 0) {
            RENAMES_1_21_11.each { from, to -> line = line.replace(from, to) }
        }
        return line
    }

    private boolean[] top(String directive) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("$directive without //#if")
        }
        return stack.last()
    }

    private boolean active() {
        return stack.every { it[0] }
    }

    private boolean parentActive() {
        return stack.size() < 2 || stack[0..-2].every { it[0] }
    }

    private boolean matches(String condition) {
        condition.trim().split(/\s+/).every { String term ->
            def m = term =~ /^(>=|<=|==|<|>)(\d+(?:\.\d+)*)$/
            if (!m.matches()) {
                throw new IllegalArgumentException("Bad condition: $term")
            }
            int c = compare(version, parse(m.group(2)))
            switch (m.group(1)) {
                case '>=': return c >= 0
                case '<=': return c <= 0
                case '==': return c == 0
                case '<': return c < 0
                default: return c > 0
            }
        }
    }

    private static List<Integer> parse(String v) {
        v.split(/\./).collect { it as int }
    }

    private static int compare(List<Integer> a, List<Integer> b) {
        for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
            int x = i < a.size() ? a[i] : 0
            int y = i < b.size() ? b[i] : 0
            if (x != y) {
                return x <=> y
            }
        }
        return 0
    }

    static void selfTest() {
        def run = { String mc, List<String> lines -> def p = new Preprocessor(mc); lines.collect { p.line(it) }.findAll { it } }
        def src = ['//#if >=1.21.6', 'a', '//#elif >=1.21.2 <1.21.5', 'b', '//#else', 'c', '//#if ==1.21', 'd', '//#endif', '//#endif', 'e']
        assert run('1.21.8', src) == ['a', 'e']
        assert run('1.21.4', src) == ['b', 'e']
        assert run('1.21.5', src) == ['c', 'e']
        assert run('1.21', src) == ['c', 'd', 'e']
        assert run('1.21.1', src) == ['c', 'e']
        assert run('1.21.11', ['ResourceLocationArgument.id()', 'key.location()']) == ['IdentifierArgument.id()', 'key.identifier()']
        assert run('1.21.10', ['ResourceLocation x']) == ['ResourceLocation x']
    }
}
