let MathElement = function( expression, complexity, tf, formulaIDs, docIDs ) {
    this.expression = expression;
    this.complexity = complexity;
    this.termFrequency = tf;
    if ( formulaIDs ) this.formulaIDs = formulaIDs.split(' ');
    if ( docIDs ) this.docIDs = docIDs.split(' ');
}

module.exports = MathElement = {
    MathElement: MathElement
}