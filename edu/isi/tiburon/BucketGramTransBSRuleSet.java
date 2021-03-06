package edu.isi.tiburon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
@deprecated 
*/
// BS rule set that is the result of forward application of an RTG on a tree (or eventually string)
// transducer. does it non-lazily ("bucket").
public class BucketGramTransBSRuleSet implements BSRuleSet {
	
	// store the rules by state
	private HashMap<Symbol, Vector<Rule>> ruletable;
	private PairSymbol startState;
	// token master set
	private RuleSet ruleset;
	private int ruleCount;
	public int getRuleCount() { return ruleCount; }
	public void reportRules() { 
		Debug.debug(true, "Created "+getRuleCount()+" rules");
	}
	// puts rules together (use GTOTF code!!)
	// add in rule ids carried over from gram and newly from trans 
	// if required, split result rules, creating new intermediate states
	// put rule or rules formed into hash map above
	private HashSet<PairSymbol> formAndAddRules(RTGRule prod, TransducerRule rule, boolean makeNorm) {
		boolean debug = true;
		HashSet<PairSymbol> descendants = new HashSet<PairSymbol>();
		// map transducer right sides to production terminals (assumed to be normal form)
		HashMap<TransducerRightSide, Symbol> varmap = new HashMap<TransducerRightSide, Symbol>();
		for (int i = 0; i < prod.getRHS().getNumChildren(); i++) {
			for (TransducerRightSide trt : rule.getTRVM().getRHS(rule.getLHS().getChild(i))) {
				varmap.put(trt, ((TreeItem)prod.getRHS()).getChild(i).getLabel());
				descendants.add(PairSymbol.get(((TreeItem)prod.getRHS()).getChild(i).getLabel(), trt.getState()));
				if(debug) Debug.debug(debug, "Mapped "+trt+" to "+varmap.get(trt));
			}
		}
		PairSymbol topState = PairSymbol.get(prod.getLHS(), rule.getState());
		double weight = rule.getSemiring().times(prod.getWeight(), rule.getWeight());
		// either create a single rule or split into normal form
		// normal form only done with tree trans
		if (makeNorm && rule instanceof TreeTransducerRule) {
			for (Rule newrule : getProductions(
					topState, 
					prod, rule, 
					((TreeTransducerRule)rule).getRHS(), 
					varmap,
					weight, true)) {
				if (debug) Debug.debug(debug, "Built rule "+newrule);
				ruleCount++;
				if (!ruletable.containsKey(newrule.getLHS()))
					ruletable.put(newrule.getLHS(), new Vector<Rule>());
				ruletable.get(newrule.getLHS()).add(newrule);
			}
		}
		else {
			if (rule instanceof TreeTransducerRule) {
				ruleCount++;
				RTGRule newrule = new RTGRule((RTGRuleSet)ruleset, topState, new TreeItem(((TreeTransducerRule) rule).getRHS(), varmap), weight, rule.getSemiring()); 
				newrule.tieToTransducerRule((Vector)prod.getTransducerRules());
				newrule.tieToTransducerRule((TreeTransducerRule)rule);
				if (debug) Debug.debug(debug, "Built rule "+newrule);

				if (!ruletable.containsKey(newrule.getLHS()))
					ruletable.put(newrule.getLHS(), new Vector<Rule>());
				ruletable.get(newrule.getLHS()).add(newrule);
			}
			else {
				ruleCount++;
				CFGRule newrule = new CFGRule((CFGRuleSet)ruleset, topState, new StringItem(((StringTransducerRule) rule).getRHS(), varmap), weight, rule.getSemiring()); 
				newrule.tieToTransducerRule((Vector)prod.getTransducerRules());
				newrule.tieToTransducerRule((StringTransducerRule)rule);
				if (debug) Debug.debug(debug, "Built rule "+newrule);
				if (!ruletable.containsKey(newrule.getLHS()))
					ruletable.put(newrule.getLHS(), new Vector<Rule>());
				ruletable.get(newrule.getLHS()).add(newrule);
			}
		}
		return descendants;
	}
	
	// eps grammar rule and trans state
	private PairSymbol formAndAddRules(RTGRule prod, Symbol transState) {
		boolean debug = true;
		PairSymbol topState = PairSymbol.get(prod.getLHS(), transState);
		PairSymbol rightState = PairSymbol.get(prod.getRHS().getLabel(), transState);
		ruleCount++;
		RTGRule newrule = new RTGRule((RTGRuleSet)ruleset, topState, new TreeItem(rightState), prod.getWeight(), prod.getSemiring()); 
		newrule.tieToTransducerRule((Vector)prod.getTransducerRules());
		if (debug) Debug.debug(debug, "Built eps rule "+newrule);
		if (!ruletable.containsKey(newrule.getLHS()))
			ruletable.put(newrule.getLHS(), new Vector<Rule>());
		ruletable.get(newrule.getLHS()).add(newrule);
		return rightState;
	}
	
	
	
	
	// recursively build normal-form RTG rules, creating new states as need be
	// isTop -- is this the initial call or the recursive call? 
	// determines whether external rule memorization happens
	private Vector<Rule> getProductions(
			Symbol state,
			RTGRule prod,
			TransducerRule rule,
			TransducerRightTree oldTree, 
			HashMap<TransducerRightSide, Symbol> varmap, 
			double weight, 
			boolean isTop) {
		// traverse down tree, building rules
		boolean debug = false;
		Vector<Rule> ret = new Vector<Rule>();
		Vector<Symbol> children = new Vector<Symbol>();
	
		if (debug) Debug.debug(debug, "Making productions from "+state+" and "+oldTree);
		// output eps case
		if (varmap.containsKey(oldTree)) {
			Debug.debug(true, "TODO: handle out-eps");
			return ret;
		}

		for (int i = 0; i < oldTree.getNumChildren(); i++) {
			TransducerRightTree child = oldTree.getChild(i);
			if (varmap.containsKey(child)) {
				children.add(PairSymbol.get(varmap.get(child), child.getState()));
			}
			// new state is created, and recursively build rules
			else {
				ProdSymbol newState = ProdSymbol.get(state, prod.hashCode(), rule.hashCode(), child.hashCode());
				//					Symbol newState = SymbolFactory.getStateSymbol();
				
				children.add(newState);

				ret.addAll(getProductions(
						newState, 
						prod, 
						rule, 
						child,
						varmap, 
						rule.getSemiring().ONE(),
						false));
			}
		}

		// build normal-form rule
		
		
		if (debug && children.size() > 0) Debug.debug(debug, "Got child vector of "+children);
		
		TreeItem rhs = new TreeItem(oldTree.getLabel(), children);
		RTGRule newrule = new RTGRule((RTGRuleSet)ruleset, state, rhs, weight, rule.getSemiring()); 
		if (isTop) {
			newrule.tieToTransducerRule((Vector)prod.getTransducerRules());
			newrule.tieToTransducerRule((TreeTransducerRule)rule);
		}
		ret.add(newrule);
		if (debug) Debug.debug(debug, "Made "+newrule);
		return ret;
		
	}
	
	public BucketGramTransBSRuleSet(BSRuleSet gram, CascadeTransducer trans, boolean makeNorm, boolean isTree) throws UnusualConditionException {
		// do forward application here
		// form composed start state
		boolean debug = false;
		startState = PairSymbol.get(gram.getStartState(), trans.getStartState());
		ruletable = new HashMap<Symbol, Vector<Rule>>();
		if (isTree)
			ruleset = new RTGRuleSet(trans.getSemiring());
		else
			ruleset = new CFGRuleSet(trans.getSemiring());
		// pending items
		Vector<PairSymbol> pending = new Vector<PairSymbol>();
		// done or queued items
		HashSet<PairSymbol> done = new HashSet<PairSymbol>();
		pending.add(startState);
		done.add(startState);
		while (!pending.isEmpty()) {
			PairSymbol state = pending.remove(0);
			PIterator<Rule> productions = gram.getBSIter(state.getLeft());
			while (productions.hasNext()) {
				RTGRule prod = (RTGRule)productions.next();
				// deal with epsilon output rules from gram!
				// TODO: this should be more easily detectable!
				Symbol label = prod.getRHS().getLabel();
				int numKids = prod.getRHS().getNumChildren();
				if (gram.isState(prod.getRHS().getLabel())) {
					PairSymbol nextState = formAndAddRules(prod, state.getRight());
					if (!done.contains(nextState)) {
						done.add(nextState);
						pending.add(nextState);
					}
					label = Symbol.getEpsilon();
					numKids = 1;
				}
				// TODO: deal with epsilon input rules from trans!
				Iterator rules = trans.getBackwardRules(state.getRight(), label, numKids).iterator();
				while (rules.hasNext()) {
					TransducerRule rule = (TransducerRule)rules.next();
					if (debug) Debug.debug(debug, "Putting "+prod+" and "+rule+" together");
					for (PairSymbol nextState : formAndAddRules(prod, rule, makeNorm)) {
						if (!done.contains(nextState)) {
							done.add(nextState);
							pending.add(nextState);
						}
					}
					
				}
			}
			if (debug) Debug.debug(debug, pending.size()+" states left");
		}
		if (debug) Debug.debug(debug, "Done with bucket");
	}
	public PIterator<Rule> getBSIter(Symbol s){
		return new WrappedPIterator<Rule>(ruletable.get(s).iterator());
	}

	public Symbol getStartState() {
		return startState;
	}

	// any pair symbol or a previously stored symbol
	public boolean isState(Symbol s) {
		return s instanceof PairSymbol || ruletable.containsKey(s);
	}
}
