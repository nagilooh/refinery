package tools.refinery.language.ide.contentassist;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.TokenSource;
import tools.refinery.language.ide.contentassist.antlr.ProblemParser;

/**
 * Code is duplicated from {@link tools.refinery.language.parser.antlr.TokenSourceInjectingProblemParser} due to
 * Xtext code generation.
 */
public class TokenSourceInjectingProblemParser extends ProblemParser {
	@Inject
	private Injector injector;

	@Override
	protected TokenSource createLexer(CharStream stream) {
		var tokenSource = super.createLexer(stream);
		injector.injectMembers(tokenSource);
		return tokenSource;
	}
}
