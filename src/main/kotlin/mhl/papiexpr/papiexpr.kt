package mhl.papiexpr

import kotlin.math.PI
import kotlin.math.round
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.ServerPlaceholderContext
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TagLikeParser
import com.notkamui.keval.Keval

object PlaceholderAPIExpr: ModInitializer {
	private var _Keval: com.notkamui.keval.Keval = Keval.create {
		includeDefault()
		function {
			name = "mod"
			arity = 2
			implementation = { args: DoubleArray -> args[0] % args[1] }
		}
		function {
			name = "rad"
			arity = 1
			implementation = { args: DoubleArray -> PI / 180 * args[0] }
		}
		function {
			name = "max"
			implementation = { args: DoubleArray -> args.max() }
		}
		function {
			name = "min"
			implementation = { args: DoubleArray -> args.min() }
		}
		function {
			name = "avg"
			implementation = { args: DoubleArray -> args.average() }
		}
	}

	private fun kMath(str: String): String
	{
		val res: Double = _Keval.eval(str)
		if (round(res) == res)
			return (res.toInt().toString())
		else
			return (res.toString())
	}

	// Nested args use ${...}, not the %...% of Placeholders.COMMON_PLACEHOLDER_PARSER.
	// Built rather than using PLACEHOLDER_USER, whose empty separator would swallow
	// placeholder arguments: ${player:equipment_slot mainhand} must split on the space.
	// The server getter, not the common one: registerCommon mirrors its entries into both maps,
	// but registerServer placeholders (playerpronouns:raw_pronouns) exist only in the server map
	// and getCommonPlaceholder has no fallback, so the common getter leaves them unexpanded.
	private val nestedParser: NodeParser = TagLikeParser.placeholder(
		TagLikeParser.Format.of("\${", "}", " "),
		ServerPlaceholderContext.SERVER_KEY,
		Placeholders.SERVER_PLACEHOLDER_GETTER
	)

	private fun parseNested(arg: String, ctx: PlaceholderContext): String
	{
		return (nestedParser.parseComponent(arg, ctx.asParserContext()).getString())
	}

	private fun parsekMathOrString(isMath: String, arg: String): String
	{
		if (isMath.trim() == "true")
			return (kMath(arg))
		return (arg.trim())
	}

	override fun onInitialize()
	{
		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "math")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing arguments")
				PlaceholderResult.value(Component.literal(kMath(parseNested(arg, ctx))))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "ifeq")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing arguments")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 8)
					throw Exception("Must have exactly 8 arguments")
				
				val a: String =
					if (args[0].trim(' ') == "true")
						kMath(args[1]) else args[1].trim()
				val b: String =
					if (args[2].trim(' ') == "true")
						kMath(args[3]) else args[3].trim()
				
				if (a == b)
					PlaceholderResult.value(Component.literal(parsekMathOrString(args[4], args[5])))
				else
					PlaceholderResult.value(Component.literal(parsekMathOrString(args[6], args[7])))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "iflt")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing arguments")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 6)
					throw Exception("Must have exactly 6 arguments")
				if (_Keval.eval(args[0]) < _Keval.eval(args[1]))
					PlaceholderResult.value(Component.literal(parsekMathOrString(args[2], args[3])))
				else
					PlaceholderResult.value(Component.literal(parsekMathOrString(args[4], args[5])))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "ifgt")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing arguments")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 6)
					throw Exception("Must have exactly 6 arguments")
				if (_Keval.eval(args[0]) > _Keval.eval(args[1]))
					PlaceholderResult.value(Component.literal(parsekMathOrString(args[2], args[3])))
				else
					PlaceholderResult.value(Component.literal(parsekMathOrString(args[4], args[5])))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "pad")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing argument")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 3)
					throw Exception("Must have exactly 3 arguments")
				val padChar: Char = args[2].trim().firstOrNull() ?: ' '
				val padCount: Int = ((_Keval.eval(args[0]).toInt() - args[1].trim().length) / 2).coerceAtLeast(0)
				val padString: String = padChar.toString().repeat(padCount)
				PlaceholderResult.value(Component.literal(padString + args[1].trim() + padString))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "padleft")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing argument")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 3)
					throw Exception("Must have exactly 3 arguments")
				val padChar: Char = args[2].trim().firstOrNull() ?: ' '
				val padCount: Int = (_Keval.eval(args[0]).toInt() - args[1].trim().length).coerceAtLeast(0)
				val padString: String = padChar.toString().repeat(padCount)
				PlaceholderResult.value(Component.literal(padString + args[1].trim()))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "padright")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing argument")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 3)
					throw Exception("Must have exactly 3 arguments")
				val padChar: Char = args[2].trim().firstOrNull() ?: ' '
				val padCount: Int = (_Keval.eval(args[0]).toInt() - args[1].trim().length).coerceAtLeast(0)
				val padString: String = padChar.toString().repeat(padCount)
				PlaceholderResult.value(Component.literal(args[1].trim() + padString))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "padmatch")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing argument")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 3)
					throw Exception("Must have exactly 3 arguments")
				val padChar: Char = args[2].trim().firstOrNull() ?: ' '
				val padCount: Int = ((args[0].trim().length - args[1].trim().length) / 2).coerceAtLeast(0)
				val padString: String = padChar.toString().repeat(padCount)
				PlaceholderResult.value(Component.literal(padString + args[1].trim() + padString))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "padmatchleft")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing argument")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 3)
					throw Exception("Must have exactly 3 arguments")
				val padChar: Char = args[2].trim().firstOrNull() ?: ' '
				val padCount: Int = (args[0].trim().length - args[1].trim().length).coerceAtLeast(0)
				val padString: String = padChar.toString().repeat(padCount)
				PlaceholderResult.value(Component.literal(padString + args[1].trim()))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}

		Placeholders.registerCommon<String>(Identifier.fromNamespaceAndPath("expr", "padmatchright")) {ctx, arg ->
			try {
				if (arg == null)
					throw Exception("Missing argument")

				val args: List<String> = parseNested(arg, ctx).split(';')
				if (args.size != 3)
					throw Exception("Must have exactly 3 arguments")
				val padChar: Char = args[2].trim().firstOrNull() ?: ' '
				val padCount: Int = (args[0].trim().length - args[1].trim().length).coerceAtLeast(0)
				val padString: String = padChar.toString().repeat(padCount)
				PlaceholderResult.value(Component.literal(args[1].trim() + padString))
			}
			catch (e: Exception) {
				PlaceholderResult.invalid(e.message)
			}
		}
	}
}