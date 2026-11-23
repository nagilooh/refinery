package tools.refinery.generator.cli.commands;

import tools.refinery.measurement.Abstraction;
import tools.refinery.measurement.MeasurementDomain;
import tools.refinery.measurement.MeasurementScenario;

public record RunConfiguration(
		MeasurementDomain domain,
		MeasurementScenario scenario,
		Abstraction abstraction,
		int size,
		int steps,
		int runs) {
}
